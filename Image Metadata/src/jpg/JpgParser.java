package jpg;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
            LOGGER.warn("File [" + getImageFile().getFileName() + "] has an incorrect extension name. Found [" + ext + "], updating to [jpg]");
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
     * Extracts raw segment data from the specified JPEG segment.
     *
     * <p>
     * This method iterates through the JPEG file stream to find the specified segment. It reads the
     * segment's length and payload, returning the raw byte data.
     * </p>
     *
     * @param stream
     *        the image stream to read from
     * @param segment
     *        the JPEG segment to search for, for example: APP1
     *
     * @return a byte array containing the segment's payload
     *
     * @throws IOException
     *         if a read error occurs or the stream reaches an unexpected end
     * @throws EOFException
     *         if the requested segment is not found before the end of the file
     * @throws IllegalStateException
     *         if the segment's reported length is zero or negative
     */
    private byte[] readRawSegmentData(ImageFileInputStream stream, JpegSegmentConstants segment) throws IOException
    {
        final JpegSegmentConstants EOS = JpegSegmentConstants.END_OF_IMAGE;

        while (true)
        {
            // Read two bytes that define a JPEG segment marker, for example: 0xFF 0xE1
            byte marker = stream.readByte();
            byte flag = stream.readByte();

            // Make sure the loop stops when it has reached the End of Image marker (0xFF, 0xD9)
            if (marker == EOS.marker && flag == EOS.flag)
            {
                throw new EOFException("Metadata segment [" + segment.description + "] is missing in file [" + getImageFile() + "]");
            }

            if (marker == segment.marker && flag == segment.flag)
            {
                // The segment length includes 2 bytes for the length itself,
                // so take out 2 to get the correct payload length
                int segmentLength = stream.readUnsignedShort() - 2;

                if (segmentLength <= 0)
                {
                    throw new IllegalStateException("Segment [" + segment.description + "] has a zero or negative length in file [" + getImageFile() + "]");
                }

                return stream.readBytes(segmentLength);
            }
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
        byte[] app1SegmentBytes = null;

        try (ImageFileInputStream jpgStream = new ImageFileInputStream(getImageFile()))
        {
            app1SegmentBytes = readRawSegmentData(jpgStream, JpegSegmentConstants.APP1_SEGMENT);

            // Validate the EXIF header
            if (app1SegmentBytes.length < JPG_EXIF_IDENTIFIER.length || !Arrays.equals(Arrays.copyOfRange(app1SegmentBytes, 0, JPG_EXIF_IDENTIFIER.length), JPG_EXIF_IDENTIFIER))
            {
                throw new IllegalStateException("The APP1 segment is missing a valid EXIF identifier");
            }

            byte[] exifPayload = Arrays.copyOfRange(app1SegmentBytes, JPG_EXIF_IDENTIFIER.length, app1SegmentBytes.length);

            metadata = TifParser.parseFromSegmentBytes(exifPayload);
        }

        catch (EOFException exc)
        {
            LOGGER.warn("Metadata information not found in file [" + getImageFile() + "]");
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
            LOGGER.warn("Metadata information has not been parsed yet.");
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
            LOGGER.error("Diagnostics failed for file [" + getImageFile() + "]", exc);
        }

        return sb.toString();
    }
}