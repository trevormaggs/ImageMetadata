package webp;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Optional;
import batch.BatchMetadataUtils;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.DigitalSignature;
import common.ImageReadErrorException;
import common.Metadata;
import common.SequentialByteReader;
import logger.LogFactory;
import tif.MetadataTIF;
import tif.TifParser;

/**
 * This program aims to read WebP image files and retrieve data structured in a series of RIFF-based
 * chunks. For metadata access, only the EXIF chunk, if present, will be processed. At this stage,
 * XMP metadata is considered for inclusion in this class on a later date.
 *
 * <p>
 * <b>WebP Data Stream</b>
 * </p>
 *
 * <p>
 * The WebP file format is built on the RIFF container, beginning with a fixed signature:
 * </p>
 *
 * <pre>
 * <code>RIFF</code> + FileSize (4 bytes) + <code>WEBP</code>
 * </pre>
 *
 * <p>
 * This is followed by a sequence of chunks. Each chunk includes:
 * </p>
 *
 * <ul>
 * <li>4 bytes: Chunk FourCC (ASCII, for example: {@code VP8 }, {@code VP8X}, {@code EXIF})</li>
 * <li>4 bytes: Chunk payload size (unsigned, little-endian)</li>
 * <li>Payload: Variable-length data</li>
 * <li>Padding: If size is odd, 1 padding byte follows (not counted in the size field)</li>
 * </ul>
 *
 * <p>
 * There are both mandatory and optional chunk types.
 * </p>
 *
 * <p>
 * <b>Core Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>{@code VP8 } – Lossy bitstream (standard)</li>
 * <li>{@code VP8L} – Lossless bitstream</li>
 * <li>{@code VP8X} – Extended format chunk (required for metadata and animation)</li>
 * </ul>
 *
 * <p>
 * <b>Optional Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>{@code EXIF} – Embedded EXIF metadata</li>
 * <li>{@code ICCP} – Embedded ICC color profile</li>
 * <li>{@code XMP } – XMP metadata</li>
 * <li>{@code ANIM} / {@code ANMF} – Animation control and frames</li>
 * </ul>
 *
 * <p>
 * <b>Chunk Processing</b>
 * </p>
 *
 * <ul>
 * <li>Only chunks specified in the {@code requiredChunks} list are read</li>
 * <li>An empty {@code requiredChunks} list disables chunk extraction</li>
 * <li>A null list results in all chunks being extracted</li>
 * </ul>
 *
 * <p>
 * When the {@code EXIF} chunk is found, its payload is parsed as TIFF/EXIF-formatted metadata. This
 * is commonly used to extract orientation, date, and camera information.
 * </p>
 *
 * @see <a href="https://developers.google.com/speed/webp/docs/riff_container">WebP RIFF Container
 *      Specification</a>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class WebpParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(WebpParser.class);
    private static final ByteOrder WEBP_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;
    private static final EnumSet<WebPChunkType> DEFAULT_METADATA_CHUNKS = EnumSet.of(WebPChunkType.EXIF);

    /**
     * This constructor creates an instance for processing the specified image file.
     *
     * @param fpath
     *        specifies the WebP file path, encapsulated in a Path object
     *
     * @throws IOException
     *         if an I/O issue arises
     */
    public WebpParser(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info("Image file [" + getImageFile() + "] loaded");

        String ext = BatchMetadataUtils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("webp"))
        {
            LOGGER.warn("File [" + getImageFile().getFileName() + "] has an incorrect extension name. Found [" + ext + "], updating to [webp]");
        }
    }

    /**
     * This constructor creates an instance for processing the specified image file.
     *
     * @param file
     *        specifies the WebP image file to be read
     *
     * @throws IOException
     *         if an I/O problem has occurred
     */
    public WebpParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Reads and parses data in the WebP image file and returns a new Metadata object.
     *
     * @return a Metadata object containing extracted metadata
     *
     * @throws ImageReadErrorException
     *         in case of processing errors
     * @throws IOException
     *         if the file is not in WebP format
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException, IOException
    {
        byte[] bytes = readAllBytes(); // Never return null

        try
        {
            if (bytes.length > 0)
            {
                // Use little-endian byte order as per Specifications
                SequentialByteReader webpReader = new SequentialByteReader(bytes, WEBP_BYTE_ORDER);

                WebpHandler handler = new WebpHandler(getImageFile(), webpReader, DEFAULT_METADATA_CHUNKS);
                handler.parseMetadata();

                Optional<byte[]> exif = handler.getExifData();

                if (exif.isPresent())
                {
                    metadata = TifParser.parseFromSegmentBytes(exif.get());
                }

                else
                {
                    LOGGER.info("No Exif block found in file [" + getImageFile() + "]");

                    /* Fallback to empty metadata */
                    metadata = new MetadataTIF();
                }
            }

            else
            {
                throw new ImageReadErrorException("WebP file [" + getImageFile() + "] is empty");
            }

            // webpReader.printRawBytes();
        }

        catch (IllegalStateException exc)
        {
            throw new ImageReadErrorException("WebP file [" + getImageFile() + "] appears corrupted", exc);
        }

        catch (NoSuchFileException exc)
        {
            throw new ImageReadErrorException("File [" + getImageFile() + "] does not exist", exc);
        }

        catch (IOException exc)
        {
            throw new ImageReadErrorException("Problem while reading the stream in file [" + getImageFile() + "]", exc);
        }

        return metadata;
    }

    /**
     * Retrieves previously parsed metadata from the WebP file.
     *
     * @return a populated {@link Metadata} object, or an empty one if no metadata was found
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
     * Returns the detected {@code WebP} format.
     *
     * @return a {@link DigitalSignature} enum constant representing this image format
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.WEBP;
    }
}