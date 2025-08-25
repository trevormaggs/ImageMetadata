package jpg;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import batch.BatchMetadataUtils;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.DigitalSignature;
import common.ImageFileInputStream;
import common.ImageReadErrorException;
import common.Metadata;
import logger.LogFactory;
import tif.DirectoryIFD;
import tif.DirectoryIFD.EntryIFD;
import tif.MetadataTIF;
import tif.TifParser;

/**
 * A parser for JPG image files that extracts metadata from the APP segments. This version is
 * updated to handle multi-segment metadata, specifically for ICC and XMP data, in addition to the
 * single-segment EXIF data.
 *
 * <p>
 * This parser adheres to the EXIF specification (version 2.32, CIPA DC-008-2019), which mandates
 * that all EXIF metadata must be contained within a single APP1 segment. The parser will search for
 * and process the first APP1 segment it encounters that contains the "Exif" identifier.
 * </p>
 *
 * <p>
 * For ICC profiles, the parser now collects and concatenates all APP2 segments that contain the
 * "ICC_PROFILE" identifier, following the concatenation rules defined in the ICC specification.
 * Similarly, for XMP data, it collects and concatenates all APP1 segments with the
 * "http://ns.adobe.com/xap/1.0/" identifier to form a single XMP data block.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.5
 * @since 25 August 2025
 */
public class JpgParserAdvanced extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(JpgParserAdvanced.class);
    public static final byte[] EXIF_IDENTIFIER = "Exif\0\0".getBytes(StandardCharsets.UTF_8);

    // Identifiers for other common metadata formats in APP segments
    public static final byte[] ICC_IDENTIFIER = "ICC_PROFILE\0".getBytes(StandardCharsets.UTF_8);
    public static final byte[] XMP_IDENTIFIER = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.UTF_8);

    private Optional<byte[]> exifMetadata = Optional.empty();
    private Optional<byte[]> xmpMetadata = Optional.empty();
    private Optional<byte[]> iccMetadata = Optional.empty();

    /**
     * A simple class to hold the raw byte arrays of the different metadata segments found.
     */
    private static class JpgSegmentData
    {
        private final Optional<byte[]> exif;
        private final Optional<byte[]> xmp;
        private final Optional<byte[]> icc;

        public JpgSegmentData(Optional<byte[]> exif, Optional<byte[]> xmp, Optional<byte[]> icc)
        {
            this.exif = exif;
            this.xmp = xmp;
            this.icc = icc;
        }

        public Optional<byte[]> getExif()
        {
            return exif;
        }

        public Optional<byte[]> getXmp()
        {
            return xmp;
        }

        public Optional<byte[]> getIcc()
        {
            return icc;
        }
    }

    /**
     * Constructs a new instance with the specified file path.
     *
     * @param fpath
     *        the path to the JPG file to be parsed
     *
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public JpgParserAdvanced(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info(String.format("Image file [%s] loaded", getImageFile()));

        String ext = BatchMetadataUtils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("jpg"))
        {
            LOGGER.warn(String.format("Incorrect extension name detected in file [%s]. Should be [jpg], but found [%s]", getImageFile().getFileName(), ext));
        }
    }

    /**
     * Constructs a new instance from a file path string.
     *
     * @param file
     *        the path to the JPG file as a string
     *
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public JpgParserAdvanced(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Reconstructs a complete XMP metadata block by concatenating multiple raw XMP segments.
     * 
     * <p>
     * The Extensible Metadata Platform (XMP) specification allows XMP data to be stored across
     * multiple APP1 segments within a JPEG file. This method reassembles these fragments into a
     * single, cohesive byte array for parsing.
     * </p>
     *
     * @param segments
     *        the list of byte arrays, each representing a raw APP1 segment containing XMP data.
     * 
     * @return an Optional containing the concatenated byte array, or returns Optional.empty() if no
     *         segments are available
     */
    private Optional<byte[]> reconstructXmpSegments(List<byte[]> segments)
    {
        if (!segments.isEmpty())
        {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
                for (byte[] seg : segments)
                {
                    baos.write(seg);
                }

                return Optional.of(baos.toByteArray());
            }

            catch (IOException exc)
            {
                LOGGER.error("Failed to concatenate XMP segments", exc);
            }
        }

        return Optional.empty();
    }

    /**
     * Reconstructs a complete XMP metadata block by concatenating multiple ICC profile segments.
     * Segments are ordered by their sequence number as specified in the header.
     *
     * @param segments
     *        the list of raw ICC segments
     * 
     * @return an Optional containing the concatenated byte array, or empty if valid segments are
     *         unavailable
     */
    private Optional<byte[]> reconstructIccProfile(List<byte[]> segments)
    {
        // The header is 14 bytes, including 2 bytes for the sequence/total count
        final int headerLength = ICC_IDENTIFIER.length + 2;

        if (!segments.isEmpty())
        {
            segments.sort(new Comparator<byte[]>()
            {
                @Override
                public int compare(byte[] s1, byte[] s2)
                {
                    return Integer.compare(s1[ICC_IDENTIFIER.length], s2[ICC_IDENTIFIER.length]);
                }
            });

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
                for (byte[] seg : segments)
                {
                    baos.write(Arrays.copyOfRange(seg, headerLength, seg.length));
                }

                return Optional.of(baos.toByteArray());
            }

            catch (IOException exc)
            {
                LOGGER.error("Failed to concatenate ICC segments", exc);
            }
        }

        return Optional.empty();
    }

    /**
     * Reads the next JPEG segment marker from the specified input stream.
     *
     * @param stream
     *        the input stream of the JPEG file, positioned at the current
     *        read location
     * @return an {@code Optional<JpegSegmentConstants>} representing the marker
     *         and its flag, or {@code Optional.empty()} if end-of-file is
     *         reached
     *
     * @throws IOException
     *         if an I/O error occurs while reading from the stream
     */
    private Optional<JpegSegmentConstants> fetchNextSegment(ImageFileInputStream stream) throws IOException
    {
        while (true)
        {
            int marker;
            int flag;

            try
            {
                marker = stream.readUnsignedByte();
            }

            catch (EOFException eof)
            {
                return Optional.empty();
            }

            if (marker != 0xFF)
            {
                // resync to marker
                continue;
            }

            try
            {
                flag = stream.readUnsignedByte();
            }

            catch (EOFException eof)
            {
                return Optional.empty();
            }

            /*
             * In some cases, JPEG allows multiple 0xFF bytes (fill or padding bytes) before the
             * actual segment flag. These are not part of any segment and should be skipped to find
             * the next true segment type. A warning is logged and parsing is terminated if an
             * excessive number of consecutive 0xFF fill bytes are found, as this may indicate a
             * malformed or corrupted file.
             */

            int fillCount = 0;

            while (flag == 0xFF)
            {
                fillCount++;

                // Arbitrary limit to prevent infinite loops
                if (fillCount > 64)
                {
                    LOGGER.warn("Excessive 0xFF padding bytes detected, possible file corruption");
                    return Optional.empty();
                }

                try
                {
                    flag = stream.readUnsignedByte();
                }

                catch (EOFException eof)
                {
                    return Optional.empty();
                }
            }

            return Optional.ofNullable(JpegSegmentConstants.fromBytes(marker, flag));
        }
    }

    /**
     * Reads all supported metadata segments (EXIF, ICC, XMP) from the JPEG file.
     *
     * @param stream
     *        the input JPEG stream
     * 
     * @return a {@link JpgSegmentData} record containing the byte arrays for any found segments
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    private JpgSegmentData readMetadataSegments(ImageFileInputStream stream) throws IOException
    {
        byte[] exifSegment = null;
        List<byte[]> iccSegments = new ArrayList<>();
        List<byte[]> xmpSegments = new ArrayList<>();

        while (true)
        {
            Optional<JpegSegmentConstants> optSeg = fetchNextSegment(stream);

            if (!optSeg.isPresent())
            {
                break;
            }

            JpegSegmentConstants segment = optSeg.get();

            if (!segment.hasLengthField())
            {
                if (segment == JpegSegmentConstants.END_OF_IMAGE || segment == JpegSegmentConstants.START_OF_STREAM)
                {
                    LOGGER.debug("End marker reached, stopping metadata parsing");
                    break;
                }
            }

            else
            {
                int length = stream.readUnsignedShort() - 2;

                if (length <= 0)
                {
                    continue;
                }

                byte[] payload = stream.readBytes(length);

                if (segment == JpegSegmentConstants.APP1_SEGMENT)
                {
                    if (payload.length >= EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, EXIF_IDENTIFIER.length), EXIF_IDENTIFIER))
                    {
                        if (exifSegment == null)
                        {
                            // The EXIF payload starts after the 6-byte "Exif\0\0" identifier
                            exifSegment = Arrays.copyOfRange(payload, EXIF_IDENTIFIER.length, payload.length);
                            LOGGER.debug(String.format("Valid EXIF APP1 segment found. Length [%d]", exifSegment.length));
                        }
                    }

                    else if (payload.length >= XMP_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, XMP_IDENTIFIER.length), XMP_IDENTIFIER))
                    {
                        xmpSegments.add(payload);
                        LOGGER.debug(String.format("Valid XMP APP1 segment found. Length [%d]", payload.length));
                    }

                    else
                    {
                        LOGGER.debug(String.format("Non-EXIF/XMP APP1 segment skipped. Length [%d]", payload.length));
                    }
                }

                else if (segment == JpegSegmentConstants.APP2_SEGMENT)
                {
                    if (payload.length >= ICC_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, ICC_IDENTIFIER.length), ICC_IDENTIFIER))
                    {
                        iccSegments.add(payload);
                        LOGGER.debug(String.format("Valid ICC APP2 segment found. Length [%d]", payload.length));
                    }

                    else
                    {
                        LOGGER.debug(String.format("Non-ICC APP2 segment skipped. Length [%d]", payload.length));
                    }
                }

                else
                {
                    LOGGER.debug(String.format("Unhandled segment [0xFF%02X] skipped. Length [%d]", segment.getFlag(), length));
                }
            }
        }

        Optional<byte[]> exifData = Optional.ofNullable(exifSegment);
        Optional<byte[]> xmpData = reconstructXmpSegments(xmpSegments);
        Optional<byte[]> iccData = reconstructIccProfile(iccSegments);

        return new JpgSegmentData(exifData, xmpData, iccData);
    }

    /**
     * Reads the metadata from a JPG file, if present, using the APP1 EXIF segment.
     *
     * @return a populated {@link Metadata} object containing the metadata
     *
     * @throws ImageReadErrorException
     *         if the file is unreadable
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException
    {
        try (ImageFileInputStream jpgStream = new ImageFileInputStream(getImageFile()))
        {
            JpgSegmentData segmentData = readMetadataSegments(jpgStream);

            exifMetadata = segmentData.getExif();
            iccMetadata = segmentData.getIcc();
            xmpMetadata = segmentData.getXmp();

            if (exifMetadata.isPresent())
            {
                metadata = TifParser.parseFromSegmentBytes(exifMetadata.get());
            }

            else
            {
                LOGGER.info("No EXIF metadata present in image");
            }

            // TODO: develop logic to support XMP and ICC metadata
            // xmpMetadata.ifPresent(xmpBytes -> parseXmp(xmpBytes));
            // iccMetadata.ifPresent(iccBytes -> parseIcc(iccBytes));
        }

        catch (NoSuchFileException exc)
        {
            throw new ImageReadErrorException("File [" + getImageFile() + "] does not exist", exc);
        }

        catch (IOException exc)
        {
            throw new ImageReadErrorException(exc);
        }

        catch (IllegalStateException exc)
        {
            throw new ImageReadErrorException("Error parsing metadata for file [" + getImageFile() + "]", exc);
        }

        return getSafeMetadata();
    }

    /**
     * Returns the previously parsed metadata from the JPG file.
     *
     * @return the metadata object, or an empty one if none was found
     */
    @Override
    public Metadata<? extends BaseMetadata> getSafeMetadata()
    {
        if (metadata == null)
        {
            LOGGER.warn("No metadata information has been parsed yet");
            return new MetadataTIF();
        }

        return metadata;
    }

    /**
     * Returns the detected {@code JPG} format.
     *
     * @return a {@link DigitalSignature} enum constant representing this image format
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.JPG;
    }

    /**
     * Generates a human-readable diagnostic string containing metadata details.
     *
     * @return a formatted string suitable for diagnostics, logging, or inspection
     */
    @Override
    public String formatDiagnosticString()
    {
        Metadata<?> meta = getSafeMetadata();
        StringBuilder sb = new StringBuilder();

        try
        {
            sb.append("\t\t\tJPG Metadata Summary").append(System.lineSeparator()).append(System.lineSeparator());
            sb.append(super.formatDiagnosticString());

            if (meta instanceof MetadataTIF && meta.hasExifData())
            {
                MetadataTIF tif = (MetadataTIF) meta;

                for (DirectoryIFD ifd : tif)
                {
                    sb.append("Directory Type - ")
                            .append(ifd.getDirectoryType().getDescription())
                            .append(String.format(" (%d entries)%n", ifd.length()))
                            .append(DIVIDER)
                            .append(System.lineSeparator());

                    for (EntryIFD entry : ifd)
                    {
                        String value = ifd.getStringValue(entry);
                        sb.append(String.format(FMT, "Tag Name", entry.getTag() + " (Tag ID: " + String.format("0x%04X", entry.getTagID()) + ")"));
                        sb.append(String.format(FMT, "Field Type", entry.getFieldType() + " (count: " + entry.getCount() + ")"));
                        sb.append(String.format(FMT, "Value", (value == null || value.isEmpty() ? "Empty" : value)));
                        sb.append(System.lineSeparator());
                    }
                }
            }

            else
            {
                sb.append("No EXIF metadata found").append(System.lineSeparator());
            }

            sb.append(System.lineSeparator()).append(DIVIDER).append(System.lineSeparator());

            if (this.iccMetadata.isPresent())
            {
                sb.append("ICC Profile Found: ").append(this.iccMetadata.get().length).append(" bytes").append(System.lineSeparator());
                sb.append("    Note: Parser has concatenated all ICC segments.").append(System.lineSeparator());
            }

            else
            {
                sb.append("No ICC Profile found.").append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());

            if (this.xmpMetadata.isPresent())
            {
                sb.append("XMP Data Found: ").append(this.xmpMetadata.get().length).append(" bytes").append(System.lineSeparator());
                sb.append("    Note: Parser has concatenated all XMP segments.").append(System.lineSeparator());
            }

            else
            {
                sb.append("No XMP Data found.").append(System.lineSeparator());
            }
        }

        catch (Exception exc)
        {
            sb.append("Error generating diagnostics: ").append(exc.getMessage()).append(System.lineSeparator());
            LOGGER.error("Diagnostics failed for file [" + getImageFile() + "]", exc);
        }

        return sb.toString();
    }
}