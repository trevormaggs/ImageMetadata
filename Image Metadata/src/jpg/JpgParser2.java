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
     * This supports reading multiple APP1 segments that contain EXIF data and reassembles them into
     * a single byte array. This method is safe for cases where EXIF data has been split across
     * multiple APP1 segments, due to the 64KB limit.
     * 
     * <p>
     * To ensure correctness, the continuity of the TIFF stream across split APP1 segments is
     * verified. If a discontinuity is detected, an exception is thrown to prevent parsing corrupted
     * EXIF data.
     *
     * @param stream
     *        input stream of the JPEG file
     * @return
     *         a reassembled EXIF payload, after reading the "Exif\0\0" header
     *
     * @throws IOException
     *         if an I/O error occurs
     * @throws EOFException
     *         if no valid EXIF APP1 segments are found
     * @throws IllegalStateException
     *         if discontinuity is detected in split EXIF segments
     */
    private Optional<byte[]> readMultipleApp1ExifSegments(ImageFileInputStream stream) throws IOException
    {
        List<byte[]> exifChunks = new ArrayList<>();
        int total = 0;

        while (true)
        {
            // --- Read marker prefix (0xFF). Allow fill bytes 0xFF 0xFF ... ---
            int marker;

            try
            {
                marker = stream.readUnsignedByte();
            }

            catch (IOException eof)
            {
                break; // true EOF
            }

            if (marker != 0xFF)
            {
                // Not synchronized on a marker; keep scanning forward until we hit 0xFF
                continue;
            }

            // Read the flag byte; skip any additional 0xFF fill bytes
            int flag;

            try
            {
                flag = stream.readUnsignedByte();
            }

            catch (IOException eof)
            {
                break;
            }

            while (flag == 0xFF)
            {
                try
                {
                    flag = stream.readUnsignedByte();
                }

                catch (IOException eof)
                {
                    flag = -1;
                    break;
                }
            }

            if (flag == -1) break; // EOF while collapsing fill bytes

            // Handle key markers without length
            if (flag == 0xD8)
            { // SOI
                continue;
            }

            if (flag == 0xD9)
            { // EOI
                break;
            }

            if (flag == 0xDA)
            { // SOS (Start of Scan)
              // Read SOS header length and skip its header bytes only.
              // After this point comes entropy-coded data; no more APP1/EXIF will appear.
                int sosLen;

                try
                {
                    sosLen = stream.readUnsignedShort();
                }

                catch (IOException eof)
                {
                    break;
                }

                int sosHdr = sosLen - 2;

                if (sosHdr > 0)
                {
                    try
                    {
                        stream.skip(sosHdr);
                    }

                    catch (IOException eof)
                    {
                        // If we can't skip the full SOS header, just stop scanning.
                    }
                }

                break; // stop scanning after SOS
            }

            // All remaining markers we care about have a 2-byte length that includes those 2 bytes
            int segLen;

            try
            {
                segLen = stream.readUnsignedShort();
            }

            catch (IOException eof)
            {
                break;
            }

            int payloadLen = segLen - 2;

            if (payloadLen < 0)
            {
                // Corrupt length; try to resync
                continue;
            }

            if (flag == 0xE1)
            { // APP1
                byte[] payload;

                try
                {
                    payload = stream.readBytes(payloadLen);
                }

                catch (IOException eof)
                {
                    break;
                }

                if (payload.length >= JPG_EXIF_IDENTIFIER.length && new String(payload, 0, JPG_EXIF_IDENTIFIER.length, StandardCharsets.US_ASCII).startsWith("Exif"))
                {
                    byte[] exif = Arrays.copyOfRange(payload, JPG_EXIF_IDENTIFIER.length, payload.length);

                    exifChunks.add(exif);
                    total += exif.length;

                    LOGGER.debug("Valid EXIF APP1 segment found. Length [" + exif.length + "]");
                }

                else
                {
                    // Non-EXIF APP1 (e.g., XMP) — ignore
                }
            }

            else
            {
                // Skip any other length-bearing segment payload
                if (payloadLen > 0)
                {
                    try
                    {
                        stream.skip(payloadLen);
                    }

                    catch (IOException eof)
                    {
                        break;
                    }
                }
            }
        }

        if (exifChunks.isEmpty())
        {
            LOGGER.info("No EXIF APP1 segments found in file [" + getImageFile() + "]");

            return Optional.empty();
        }

        // Reassemble segments into one contiguous buffer
        ByteArrayOutputStream exifBuffer = new ByteArrayOutputStream(total);

        for (byte[] seg : exifChunks)
        {
            exifBuffer.write(seg, 0, seg.length);
        }

        return Optional.of(exifBuffer.toByteArray());
    }

    /**
     * Reads the APP1 segment of the JPEG file, searching for an EXIF block and stopping at the
     * first valid EXIF APP1 and skips the rest.
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
    private Optional<byte[]> readFirstApp1ExifSegment(ImageFileInputStream stream) throws IOException
    {
        while (true)
        {
            // Read two bytes that define a JPEG segment marker, for example: 0xFF 0xE1
            byte marker = stream.readByte();
            byte flag = stream.readByte();

            if (JpegSegmentConstants.fromBytes(marker, flag) == JpegSegmentConstants.END_OF_IMAGE)
            {
                throw new EOFException("No valid EXIF APP1 segment found in file [" + getImageFile() + "]");
            }

            if (JpegSegmentConstants.fromBytes(marker, flag) == JpegSegmentConstants.APP1_SEGMENT)
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
                    // return Arrays.copyOfRange(segmentBytes, JPG_EXIF_IDENTIFIER.length,
                    // segmentBytes.length);
                    return Optional.of(Arrays.copyOfRange(segmentBytes, JPG_EXIF_IDENTIFIER.length, segmentBytes.length));
                }

                else
                {
                    LOGGER.debug("Non-EXIF APP1 segment found. Skipped");
                }
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

    public Metadata<? extends BaseMetadata> readMetadata2() throws ImageReadErrorException
    {
        try (ImageFileInputStream jpgStream = new ImageFileInputStream(getImageFile()))
        {
            // byte[] exifPayload = (MULTI_EXIF_SEGMENT ? readMultipleApp1ExifSegments(jpgStream) :
            // readFirstApp1ExifSegment(jpgStream));

            Optional<byte[]> exif = (MULTI_EXIF_SEGMENT ? readMultipleApp1ExifSegments(jpgStream) : readFirstApp1ExifSegment(jpgStream));

            if (exif.isPresent())
            {
                metadata = TifParser.parseFromSegmentBytes(exif.get());
            }

            else
            {
                LOGGER.info("No EXIF metadata present in image");
            }

            // metadata = TifParser.parseFromSegmentBytes(exifPayload);
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

    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException
    {
        try (ImageFileInputStream jpgStream = new ImageFileInputStream(getImageFile()))
        {
            // Unified reading of EXIF APP1 segments
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
     * Reads the APP1 segments of the JPEG file, searching for EXIF blocks.
     * Can stop at the first segment (single) or collect all segments (multi).
     *
     * @param stream
     *        input stream of the JPEG file
     * @param readAll
     *        true = read all EXIF APP1 segments; false = stop at first
     * @return Optional containing the concatenated EXIF payload(s)
     * @throws IOException
     *         if an I/O error occurs
     */
    private Optional<byte[]> readApp1ExifSegments(ImageFileInputStream stream, boolean readAll) throws IOException
    {
        List<byte[]> exifChunks = new ArrayList<>();
        int total = 0;

        while (true)
        {
            int marker;
            try
            {
                marker = stream.readUnsignedByte();
            }
            catch (IOException eof)
            {
                break; // EOF
            }

            if (marker != 0xFF) continue; // resync to marker

            int flag;
            try
            {
                flag = stream.readUnsignedByte();
            }
            catch (IOException eof)
            {
                break;
            }

            while (flag == 0xFF)
            {
                try
                {
                    flag = stream.readUnsignedByte();
                }
                catch (IOException eof)
                {
                    flag = -1;
                    break;
                }
            }

            if (flag == -1) break;

            // Markers without payload
            if (flag == 0xD8) continue; // SOI
            if (flag == 0xD9) break; // EOI

            if (flag == 0xDA)
            { // SOS
                int sosLen = stream.readUnsignedShort();
                stream.skip(sosLen - 2);
                break; // stop scanning after SOS
            }

            int segLen = stream.readUnsignedShort();
            int payloadLen = segLen - 2;
            if (payloadLen < 0) continue;

            if (flag == 0xE1)
            { // APP1
                byte[] payload = stream.readBytes(payloadLen);

                if (payload.length >= JPG_EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(payload, JPG_EXIF_IDENTIFIER.length), JPG_EXIF_IDENTIFIER))
                {
                    byte[] exif = Arrays.copyOfRange(payload, JPG_EXIF_IDENTIFIER.length, payload.length);

                    exifChunks.add(exif);
                    total += exif.length;

                    LOGGER.debug("Valid EXIF APP1 segment found. Length [" + exif.length + "]");

                    if (!readAll) break; // stop at first segment
                }
            }

            else
            {
                if (payloadLen > 0) stream.skip(payloadLen);
            }
        }

        if (exifChunks.isEmpty())
        {
            LOGGER.info("No EXIF APP1 segments found in file [" + getImageFile() + "]");
            return Optional.empty();
        }

        if (exifChunks.size() == 1)
        {
            return Optional.of(exifChunks.get(0));
        }

        ByteArrayOutputStream exifBuffer = new ByteArrayOutputStream(total);

        for (byte[] seg : exifChunks)
        {
            exifBuffer.write(seg, 0, seg.length);
        }

        return Optional.of(exifBuffer.toByteArray());
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