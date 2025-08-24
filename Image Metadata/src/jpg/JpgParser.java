package jpg;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
 * A parser for JPG image files that extracts metadata from the APP1 segment, specifically targeting
 * embedded EXIF data. This data is processed using an internal TIFF parser to provide a structured
 * metadata representation.
 *
 * <p>
 * Currently, this parser supports only the extraction of EXIF metadata. It expects well-formed APP1
 * segments beginning with the "Exif\0\0" identifier.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.2
 * @since 20 August 2025
 */
public class JpgParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(JpgParser.class);
    private static final boolean MULTI_EXIF_SEGMENT = true;
    public static final byte[] JPG_EXIF_IDENTIFIER = "Exif\0\0".getBytes();

    /**
     * Constructs a new instance with the specified file path.
     *
     * @param fpath
     *        the path to the JPG file to be parsed
     *
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public JpgParser(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info("Image file [" + getImageFile() + "] loaded");

        String ext = BatchMetadataUtils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("jpg"))
        {
            LOGGER.warn("Incorrect extension name detected in file [" + getImageFile().getFileName() + "]. Should be [jpg], but found [" + ext + "]");
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
    public JpgParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Reads the next JPEG segment marker from the specified input stream.
     *
     * <p>
     * This method scans forward until it finds a valid marker sequence (0xFF followed by a non-0xFF
     * flag). It does <strong>not</strong> read the segment payload, only the marker and flag bytes.
     * </p>
     *
     * @param stream
     *        the input stream of the JPEG file, positioned at the current read location
     * @return an {@code Optional<JpegSegmentConstants>} representing the marker (always 0xFF) and
     *         its flag, or {@code Optional.empty()} if end-of-file is reached
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
                continue; // resync to marker
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
             * In some cases, JPEG allows multiple 0xFF bytes (fill or padding bytes)
             * before the actual segment flag. These are not part of any segment and
             * should be skipped to find the next true segment type.
             */
            while (flag == 0xFF)
            {
                try
                {
                    flag = stream.readUnsignedByte();
                }

                catch (EOFException eof)
                {
                    return Optional.empty();
                }
            }

            return Optional.ofNullable(JpegSegmentConstants.fromBytes((byte) marker, (byte) flag));
        }
    }

    /**
     * Reads the APP1 segments from a JPEG file, extracting EXIF metadata blocks.
     *
     * <p>
     * JPEG files may contain one or more APP1 segments with EXIF data. This method can either:
     * </p>
     *
     * <ul>
     * <li>Stop after reading the first valid EXIF APP1 segment ({@code readAll = false}), or</li>
     * <li>Collect and concatenate all EXIF APP1 segments ({@code readAll = true})</li>
     * </ul>
     *
     * <p>
     * Each APP1 segment is verified to start with the {@code "Exif\0\0"} identifier before
     * inclusion. Non-EXIF APP1 segments and other segment types are skipped.
     * </p>
     *
     * @param stream
     *        the input stream of the JPEG file, positioned at the current read location
     * @param readAll
     *        if true, reads all EXIF APP1 segments and concatenates them;
     *        if false, stops after the first valid EXIF APP1 segment
     * @return an Optional containing the concatenated EXIF payload bytes, or Optional.empty() if no
     *         valid EXIF segments are found
     *
     * @throws IOException
     *         if an I/O error occurs while reading the stream
     */
    private Optional<byte[]> readApp1ExifSegments(ImageFileInputStream stream, boolean readAll) throws IOException
    {
        int totalLength = 0;
        List<byte[]> exifSegments = new ArrayList<>();

        while (true)
        {
            Optional<JpegSegmentConstants> optSeg = fetchNextSegment(stream);

            if (!optSeg.isPresent())
            {
                break;
            }

            JpegSegmentConstants segment = optSeg.get();

            if (segment == JpegSegmentConstants.UNKNOWN_SEGMENT)
            {
                // Unknown segment - skip any existing payload
                int len = stream.readUnsignedShort() - 2;

                if (len > 0)
                {
                    stream.skip(len);
                }

                LOGGER.debug("Skipping unknown segment [0xFF" + String.format("%02X", segment.flag & 0xFF) + "] and length [" + len + "]");

                continue;
            }

            else if (segment == JpegSegmentConstants.START_OF_IMAGE)
            {
                // SOI
                continue;
            }

            else if (segment == JpegSegmentConstants.END_OF_IMAGE)
            {
                // EOI
                break;
            }

            else if (segment == JpegSegmentConstants.START_OF_STREAM)
            {
                // SOS
                int sosLen = stream.readUnsignedShort() - 2;

                if (sosLen > 0)
                {
                    stream.skip(sosLen);
                }

                break;
            }

            else
            {
                int length = stream.readUnsignedShort() - 2;

                if (length < 0)
                {
                    // corrupt segment, try to resync
                    continue;
                }

                if (segment == JpegSegmentConstants.APP1_SEGMENT)
                {
                    byte[] payload = stream.readBytes(length);

                    if (payload.length >= JPG_EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(payload, JPG_EXIF_IDENTIFIER.length), JPG_EXIF_IDENTIFIER))
                    {
                        byte[] exif = Arrays.copyOfRange(payload, JPG_EXIF_IDENTIFIER.length, payload.length);

                        exifSegments.add(exif);
                        totalLength += exif.length;

                        LOGGER.debug("Valid EXIF APP1 segment found. Length [" + exif.length + "]");

                        if (!readAll)
                        {
                            return Optional.of(exif); // stop at first segment
                        }
                    }

                    else
                    {
                        LOGGER.debug("Skipping non-EXIF APP1 segment [0xFF" + String.format("%02X", segment.flag & 0xFF) + "] length [" + length + "]");
                    }
                }

                else if (length > 0)
                {
                    // skip other segments
                    stream.skip(length);
                    LOGGER.debug("Skipping segment [" + segment.description + "] [0xFF" + String.format("%02X", segment.flag & 0xFF) + "] length [" + length + "]");
                }
            }

            if (segment == JpegSegmentConstants.END_OF_IMAGE || segment == JpegSegmentConstants.START_OF_STREAM)
            {
                // stop parsing
                break;
            }
        }

        if (exifSegments.isEmpty())
        {
            LOGGER.info("No EXIF APP1 segments found in file [" + getImageFile() + "]");
            return Optional.empty();
        }

        else if (exifSegments.size() == 1)
        {
            return Optional.of(exifSegments.get(0));
        }

        else
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(totalLength);

            for (byte[] seg : exifSegments)
            {
                baos.write(seg, 0, seg.length);
            }

            return Optional.of(baos.toByteArray());
        }
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
            Optional<byte[]> exif = readApp1ExifSegments(jpgStream, MULTI_EXIF_SEGMENT);

            if (exif.isPresent())
            {
                metadata = TifParser.parseFromSegmentBytes(exif.get());
            }

            else
            {
                LOGGER.info("No EXIF metadata present in image");
            }
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
     * <p>
     * Currently this includes EXIF directory types, entry tags, field types, counts, and values.
     * </p>
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
        }

        catch (Exception exc)
        {
            sb.append("Error generating diagnostics: ").append(exc.getMessage()).append(System.lineSeparator());
            LOGGER.error("Diagnostics failed for file [" + getImageFile() + "]", exc);
        }

        return sb.toString();
    }

    // TESTING
    private Optional<byte[]> readApp1ExifSegments2(ImageFileInputStream stream, boolean readAll) throws IOException
    {
        int totalLength = 0;
        List<byte[]> exifSegments = new ArrayList<>();

        while (true)
        {
            Optional<JpegSegmentConstants> optSeg = fetchNextSegment(stream);

            if (!optSeg.isPresent())
            {
                break;
            }

            JpegSegmentConstants segment = optSeg.get();

            switch (segment)
            {
                case START_OF_IMAGE:
                // SOI - do nothing
                break;

                case END_OF_IMAGE:
                case START_OF_STREAM:
                    // EOI or SOS - stop parsing
                    if (segment == JpegSegmentConstants.START_OF_STREAM)
                    {
                        int sosLen = stream.readUnsignedShort() - 2;

                        if (sosLen > 0)
                        {
                            stream.skip(sosLen);
                        }
                    }
                    return exifSegments.isEmpty() ? Optional.empty() : exifSegments.size() == 1 ? Optional.of(exifSegments.get(0)) : Optional.of(concatenateSegments(exifSegments, totalLength));

                case APP1_SEGMENT:
                    int length = stream.readUnsignedShort() - 2;
                    if (length < 0)
                     {
                        break; // corrupt segment
                    }

                    byte[] payload = stream.readBytes(length);
                    if (payload.length >= JPG_EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(payload, JPG_EXIF_IDENTIFIER.length), JPG_EXIF_IDENTIFIER))
                    {
                        byte[] exif = Arrays.copyOfRange(payload, JPG_EXIF_IDENTIFIER.length, payload.length);

                        exifSegments.add(exif);
                        totalLength += exif.length;

                        LOGGER.debug("Valid EXIF APP1 segment found. Length [" + exif.length + "]");

                        if (!readAll)
                        {
                            return Optional.of(exif);
                        }
                    }

                    else
                    {
                        LOGGER.debug("Skipping non-EXIF APP1 segment [0xFF" +
                                String.format("%02X", segment.flag & 0xFF) + "] length [" + length + "]");
                    }

                break;

                case UNKNOWN_SEGMENT:
                    int unkLength = stream.readUnsignedShort() - 2;
                    if (unkLength > 0)
                    {
                        stream.skip(unkLength);
                    }

                    LOGGER.debug("Skipping unknown segment [0xFF" + String.format("%02X", segment.flag & 0xFF) + "] and length [" + unkLength + "]");
                break;

                default:
                    int otherLen = stream.readUnsignedShort() - 2;

                    if (otherLen > 0)
                    {
                        stream.skip(otherLen);
                        LOGGER.debug("Skipping segment [" + segment.description + "] [0xFF" +
                                String.format("%02X", segment.flag & 0xFF) + "] length [" + otherLen + "]");
                    }

                break;
            }
        }

        if (exifSegments.isEmpty())
        {
            LOGGER.info("No EXIF APP1 segments found in file [" + getImageFile() + "]");
            return Optional.empty();
        }

        if (exifSegments.size() == 1)
        {
            return Optional.of(exifSegments.get(0));
        }

        return Optional.of(concatenateSegments(exifSegments, totalLength));
    }

    /**
     * Helper method to concatenate multiple byte arrays.
     */
    private byte[] concatenateSegments(List<byte[]> segments, int totalLength)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(totalLength);

        for (byte[] seg : segments)
        {
            try
            {
                baos.write(seg);
            }

            catch (IOException e)
            {
                // This should not happen with ByteArrayOutputStream
                LOGGER.error("Error concatenating EXIF segments", e);
            }
        }

        return baos.toByteArray();
    }
}