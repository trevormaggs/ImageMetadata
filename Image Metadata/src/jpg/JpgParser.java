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
 * Currently, this parser supports only the extraction of EXIF (APP1) metadata.
 * 
 * <p>
 * Note: EXIF metadata is always stored in a single APP1 segment. Concatenating multiple APP1
 * segments is non-standard. If support for multi-segment metadata is required, it will only apply
 * to ICC profiles (APP2) and possibly XMP (APP1) in the future.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.2
 * @since 20 August 2025
 */
public class JpgParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(JpgParser.class);
    private static final boolean MULTI_EXIF_SEGMENT = false;
    public static final byte[] EXIF_IDENTIFIER = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);

    // May be useful when extending to parse ICC Profile and XMP (Adobe)
    public static final byte[] ICC_IDENTIFIER = "ICC_PROFILE".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] XMP_IDENTIFIER = "http://ns.adobe.com/xap/1.0/".getBytes(StandardCharsets.US_ASCII);

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

            return Optional.ofNullable(JpegSegmentConstants.fromBytes(marker, flag));
        }
    }

    /**
     * Reads all APP1 segments containing EXIF data.
     *
     * @param stream
     *        the input JPEG stream
     * @param readAll
     *        whether to read all EXIF APP1 segments or stop at the first
     * 
     * @return Optional of concatenated EXIF bytes
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    private Optional<byte[]> readApp1ExifSegments(ImageFileInputStream stream, boolean readAll) throws IOException
    {
        int length;
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

            if (!segment.hasLengthField())
            {
                if (segment == JpegSegmentConstants.START_OF_IMAGE)
                {
                    continue;
                }

                else if (segment == JpegSegmentConstants.END_OF_IMAGE)
                {
                    LOGGER.debug("EOI marker reached, stopping metadata parsing");
                    break;
                }

                else if (segment == JpegSegmentConstants.START_OF_STREAM)
                {
                    LOGGER.debug("SOS marker reached, stopping metadata parsing");
                    break;
                }

                else
                {
                    LOGGER.debug(String.format("Marker [0xFF%02X] has no length, skipping", segment.getFlag()));
                    continue;
                }
            }

            if (segment == JpegSegmentConstants.APP1_SEGMENT)
            {
                length = stream.readUnsignedShort() - 2;

                if (length <= 0)
                {
                    continue;
                }

                byte[] payload = stream.readBytes(length);

                if (payload.length >= JpgParser.EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOfRange(payload, 0, JpgParser.EXIF_IDENTIFIER.length), JpgParser.EXIF_IDENTIFIER))
                {
                    byte[] exif = Arrays.copyOfRange(payload, JpgParser.EXIF_IDENTIFIER.length, payload.length);

                    exifSegments.add(exif);
                    totalLength += exif.length;

                    LOGGER.debug(String.format("Valid EXIF APP1 segment found. Length [%d]", exif.length));

                    if (!readAll)
                    {
                        return Optional.of(exif);
                    }
                }

                else
                {
                    LOGGER.debug(String.format("Non-EXIF or unhandled segment [0xFF%02X] skipped", segment.getFlag()));
                }
            }

            else
            {
                // skip unknown or other APPn segments
                length = stream.readUnsignedShort() - 2;

                if (length > 0)
                {
                    stream.skip(length);
                }

                LOGGER.debug(String.format("Non-EXIF APP1 segment [0xFF%02X] skipped", (segment.getFlag() & 0xFF)));
            }
        }

        if (exifSegments.isEmpty())
        {
            return Optional.empty();
        }

        if (exifSegments.size() == 1)
        {
            return Optional.of(exifSegments.get(0));
        }

        // Concatenate multiple EXIF segments if required
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(totalLength))
        {
            for (byte[] seg : exifSegments)
            {
                baos.write(seg);
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
}