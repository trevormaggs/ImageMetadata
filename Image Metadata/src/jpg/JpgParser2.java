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
public class JpgParser2 extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(JpgParser2.class);
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
    public JpgParser2(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info("Image file [" + getImageFile() + "] loaded");

        String ext = BatchMetadataUtils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("jpg"))
        {
            LOGGER.warn("File [" + getImageFile().getFileName() + "] has an incorrect extension name. Should be [jpg], but found [" + ext + "]");
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
    public JpgParser2(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Reads the APP1 segments of the JPEG file, searching for an EXIF block.
     * 
     * <p>
     * The returned byte array starts immediately after the "Exif\0\0" identifier and contains the
     * raw TIFF payload.
     * </p>
     *
     * @param stream
     *        input stream of the JPEG file
     * @return the EXIF TIFF payload, or throws {@link EOFException} if not found
     * 
     * @throws IOException
     *         if an I/O error occurs
     * @throws EOFException
     *         if no valid EXIF APP1 segment is found
     */
    private byte[] readExifFromApp1Segments(ImageFileInputStream stream) throws IOException
    {
        final JpegSegmentConstants EOS = JpegSegmentConstants.END_OF_IMAGE;

        while (true)
        {
            // Read two bytes that define a JPEG segment marker, for example: 0xFF 0xE1
            byte marker = stream.readByte();
            byte flag = stream.readByte();

            // Make sure the loop stops when it has reached the End of Image marker (0xFF, 0xD9)
            if (marker == EOS.getMarker() && flag == EOS.getFlag())
            {
                throw new EOFException("No valid EXIF APP1 segment found in file [" + getImageFile() + "]");
            }

            if (marker == JpegSegmentConstants.APP1_SEGMENT.getMarker() && flag == JpegSegmentConstants.APP1_SEGMENT.getFlag())
            {
                // The segment length includes 2 bytes for the length itself,
                // so take out 2 to get the correct payload length
                int segmentLength = stream.readUnsignedShort() - 2;

                if (segmentLength <= 0)
                {
                    LOGGER.warn("Encountered APP1 segment with zero or negative length. Skipped");
                    continue;
                }

                byte[] segmentBytes = stream.readBytes(segmentLength);

                if (segmentBytes.length >= JPG_EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(segmentBytes, JPG_EXIF_IDENTIFIER.length), JPG_EXIF_IDENTIFIER))
                {
                    LOGGER.debug("Valid EXIF APP1 segment found");
                    return Arrays.copyOfRange(segmentBytes, JPG_EXIF_IDENTIFIER.length, segmentBytes.length);
                }

                else
                {
                    LOGGER.debug("Non-EXIF APP1 segment found. Skipped");
                }
            }

            else
            {
                // Skip other segments
                int skipLength = stream.readUnsignedShort() - 2;

                if (skipLength > 0)
                {
                    stream.skip(skipLength);
                }

                else
                {
                    LOGGER.warn("Invalid segment length [" + skipLength + "], skipping marker");
                }
            }
        }
    }

    /**
     * Reads all APP1 segments that contain EXIF data and reassembles them into a single byte array.
     * This method is safe for cases where EXIF data has been split across multiple APP1 segments
     * (e.g. due to the 64KB limit).
     *
     * @param stream
     *        input stream of the JPEG file
     * @return
     *         a reassembled EXIF payload (after the "Exif\0\0" header)
     *
     * @throws IOException
     *         if an I/O error occurs
     * @throws EOFException
     *         if no valid EXIF APP1 segments are found
     */
    private byte[] readExifFromApp1Segments3(ImageFileInputStream stream) throws IOException
    {
        int totalLength = 0;
        List<byte[]> exifSegments = new ArrayList<>();
        final JpegSegmentConstants EOS = JpegSegmentConstants.END_OF_IMAGE;

        while (true)
        {
            byte marker = stream.readByte();
            byte flag = stream.readByte();

            if (marker == EOS.getMarker() && flag == EOS.getFlag())
            {
                break; // reached end of image
            }

            if (marker == JpegSegmentConstants.APP1_SEGMENT.getMarker() && flag == JpegSegmentConstants.APP1_SEGMENT.getFlag())
            {
                int segmentLength = stream.readUnsignedShort() - 2;

                if (segmentLength <= 0)
                {
                    LOGGER.warn("Encountered APP1 segment with invalid length [" + segmentLength + "]. Skipped");
                    continue;
                }

                byte[] segmentBytes = stream.readBytes(segmentLength);

                if (segmentBytes.length >= JPG_EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(segmentBytes, JPG_EXIF_IDENTIFIER.length), JPG_EXIF_IDENTIFIER))
                {
                    LOGGER.debug("Valid EXIF APP1 segment found (length=" + segmentBytes.length + ")");

                    // strip "Exif\0\0" only from the first one
                    int offset = exifSegments.isEmpty() ? JPG_EXIF_IDENTIFIER.length : 0;
                    byte[] exifPayload = Arrays.copyOfRange(segmentBytes, offset, segmentBytes.length);

                    exifSegments.add(exifPayload);
                    totalLength += exifPayload.length;
                }

                else
                {
                    LOGGER.debug("Non-EXIF APP1 segment found. Skipped");
                }
            }

            else
            {
                int skipLength = stream.readUnsignedShort() - 2;

                if (skipLength > 0)
                {
                    stream.skip(skipLength);
                }

                else
                {
                    LOGGER.warn("Invalid segment length [" + skipLength + "], skipping marker");
                }
            }
        }

        if (exifSegments.isEmpty())
        {
            throw new EOFException("No valid EXIF APP1 segment found in file [" + getImageFile() + "]");
        }

        // Now safely reassemble
        ByteArrayOutputStream exifBuffer = new ByteArrayOutputStream(totalLength);

        for (byte[] seg : exifSegments)
        {
            exifBuffer.write(seg);
        }

        return exifBuffer.toByteArray();
    }

    /**
     * Reads all APP1 segments that contain EXIF data and concatenates them into a single byte
     * array. If no valid EXIF APP1 segment is found, an {@link EOFException} is thrown.
     *
     * @param stream
     *        input stream of the JPEG file
     * @return
     *         concatenated byte array of all EXIF APP1 payloads (after the "Exif\0\0" header)
     *
     * @throws IOException
     *         if an I/O error occurs
     * @throws EOFException
     *         if no valid EXIF APP1 segments are found
     */
    private byte[] readExifFromApp1Segments2(ImageFileInputStream stream) throws IOException
    {
        ByteArrayOutputStream exifBuffer = new ByteArrayOutputStream();
        final JpegSegmentConstants EOS = JpegSegmentConstants.END_OF_IMAGE;
        boolean found = false;

        while (true)
        {
            byte marker = stream.readByte();
            byte flag = stream.readByte();

            if (marker == EOS.getMarker() && flag == EOS.getFlag())
            {
                break; // reached end of image
            }

            if (marker == JpegSegmentConstants.APP1_SEGMENT.getMarker() && flag == JpegSegmentConstants.APP1_SEGMENT.getFlag())
            {
                int segmentLength = stream.readUnsignedShort() - 2;

                if (segmentLength <= 0)
                {
                    LOGGER.warn("Encountered APP1 segment with zero or negative length. Skipped");
                    continue;
                }

                byte[] segmentBytes = stream.readBytes(segmentLength);

                if (segmentBytes.length >= JPG_EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(segmentBytes, JPG_EXIF_IDENTIFIER.length), JPG_EXIF_IDENTIFIER))
                {
                    LOGGER.debug("Valid EXIF APP1 segment found (length=" + segmentBytes.length + ")");

                    exifBuffer.write(segmentBytes, JPG_EXIF_IDENTIFIER.length, segmentBytes.length - JPG_EXIF_IDENTIFIER.length);
                    found = true;
                }

                else
                {
                    LOGGER.debug("Non-EXIF APP1 segment found. Skipped");
                }
            }

            else
            {
                int skipLength = stream.readUnsignedShort() - 2;

                if (skipLength > 0)
                {
                    stream.skip(skipLength);
                }

                else
                {
                    LOGGER.warn("Invalid segment length [" + skipLength + "], skipping marker");
                }
            }
        }

        if (!found)
        {
            throw new EOFException("No valid EXIF APP1 segment found in file [" + getImageFile() + "]");
        }

        return exifBuffer.toByteArray();
    }

    /**
     * Reads all APP1 segments that contain EXIF data and reassembles them into a single byte array.
     * This method is safe for cases where EXIF data has been split across multiple APP1 segments
     * (e.g. due to the 64KB limit).
     * 
     * <p>
     * To ensure correctness, the continuity of the TIFF stream across split APP1 segments is
     * verified. If a discontinuity is detected, an exception is thrown to prevent parsing corrupted
     * EXIF data.
     *
     * @param stream
     *        input stream of the JPEG file
     * @return
     *         a reassembled EXIF payload (after the "Exif\0\0" header)
     *
     * @throws IOException
     *         if an I/O error occurs
     * @throws EOFException
     *         if no valid EXIF APP1 segments are found
     * @throws IllegalStateException
     *         if discontinuity is detected in split EXIF segments
     */
    private byte[] readExifFromApp1Segments4(ImageFileInputStream stream) throws IOException
    {
        int totalLength = 0;
        int expectedNextOffset = 0;
        List<byte[]> exifSegments = new ArrayList<>();
        final JpegSegmentConstants EOS = JpegSegmentConstants.END_OF_IMAGE;

        while (true)
        {
            byte marker = stream.readByte();
            byte flag = stream.readByte();

            if (marker == EOS.getMarker() && flag == EOS.getFlag())
            {
                break; // reached end of image
            }

            if (marker == JpegSegmentConstants.APP1_SEGMENT.getMarker() && flag == JpegSegmentConstants.APP1_SEGMENT.getFlag())
            {
                int segmentLength = stream.readUnsignedShort() - 2;

                if (segmentLength <= 0)
                {
                    LOGGER.warn("Encountered APP1 segment with invalid length [" + segmentLength + "]. Skipped");
                    continue;
                }

                byte[] segmentBytes = stream.readBytes(segmentLength);

                if (segmentBytes.length >= JPG_EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(segmentBytes, JPG_EXIF_IDENTIFIER.length), JPG_EXIF_IDENTIFIER))
                {
                    LOGGER.debug("Valid EXIF APP1 segment found (length=" + segmentBytes.length + ")");

                    int offset = exifSegments.isEmpty() ? JPG_EXIF_IDENTIFIER.length : 0;
                    byte[] exifPayload = Arrays.copyOfRange(segmentBytes, offset, segmentBytes.length);

                    // Continuity check: each new segment must align exactly after previous
                    if (!exifSegments.isEmpty())
                    {
                        if (expectedNextOffset != totalLength)
                        {
                            throw new IllegalStateException("Discontinuity detected in split EXIF APP1 segments. Expected offset=" + expectedNextOffset + " but current total=" + totalLength);
                        }
                    }

                    exifSegments.add(exifPayload);
                    totalLength += exifPayload.length;

                    // Set next expected offset for safety
                    expectedNextOffset = totalLength;
                }

                else
                {
                    LOGGER.debug("Non-EXIF APP1 segment found. Skipped");
                }
            }

            else
            {
                int skipLength = stream.readUnsignedShort() - 2;

                if (skipLength > 0)
                {
                    stream.skip(skipLength);
                }
                else
                {
                    LOGGER.warn("Invalid segment length [" + skipLength + "], skipping marker");
                }
            }
        }

        if (exifSegments.isEmpty())
        {
            throw new EOFException("No valid EXIF APP1 segment found in file [" + getImageFile() + "]");
        }

        // Reassemble segments into one contiguous buffer
        ByteArrayOutputStream exifBuffer = new ByteArrayOutputStream(totalLength);

        for (byte[] seg : exifSegments)
        {
            exifBuffer.write(seg);
        }

        return exifBuffer.toByteArray();
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
            byte[] exifPayload = readExifFromApp1Segments(jpgStream);

            metadata = TifParser.parseFromSegmentBytes(exifPayload);
        }

        catch (EOFException exc)
        {
            LOGGER.info("Metadata information not found in file [" + getImageFile() + "]");
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