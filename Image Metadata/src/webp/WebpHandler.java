package webp;

import static webp.WebPChunkType.RIFF;
import static webp.WebPChunkType.VP8;
import static webp.WebPChunkType.VP8L;
import static webp.WebPChunkType.VP8X;
import static webp.WebPChunkType.WEBP;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import common.ByteValueConverter;
import common.ImageHandler;
import common.ImageReadErrorException;
import common.SequentialByteReader;
import jpg.JpgParser;
import logger.LogFactory;

/**
 * Handles the processing and collection of chunks, including EXIF metadata chunk, from a WEBP image
 * file.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 1 August 2025
 */
public class WebpHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(WebpHandler.class);
    private static final EnumSet<WebPChunkType> FIRST_CHUNK_TYPES = EnumSet.of(VP8, VP8L, VP8X);
    private final SequentialByteReader reader;
    private final List<WebpChunk> chunks;
    private final EnumSet<WebPChunkType> requiredChunks;
    private final int fileSize;

    /**
     * This default constructor should not be invoked, or it will throw an exception to prevent
     * instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    public WebpHandler()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Constructs a handler to parse selected chunks from a WebP image file.
     * 
     * @param reader
     *        byte reader for raw WebP stream
     * @param requiredChunks
     *        an optional set of chunk types to be extracted (null means all chunks are selected)
     *
     * @throws IllegalStateException
     *         if the WebP header information is corrupted
     */
    public WebpHandler(SequentialByteReader reader, EnumSet<WebPChunkType> requiredChunks)
    {
        this.reader = reader;
        this.chunks = new ArrayList<>();
        this.requiredChunks = requiredChunks;
        this.fileSize = readFileHeader(reader);
    }

    /**
     * Retrieves a list of chunks that have been extracted.
     *
     * @return an unmodified list of chunks
     */
    public List<WebpChunk> getChunks()
    {
        return Collections.unmodifiableList(chunks);
    }

    /**
     * Checks if a chunk with the specified type has already been set.
     *
     * @param type
     *        the type of the chunk
     *
     * @return true if the chunk is already present
     */
    public boolean existsChunk(WebPChunkType type)
    {
        for (WebpChunk chunk : chunks)
        {
            if (chunk.getType() == type)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the embedded EXIF data from the WebP file, if present.
     *
     * <p>
     * The EXIF metadata is stored in the {@code EXIF} chunk as raw TIFF-formatted data. If the WebP
     * file contains an {@code EXIF} chunk, its byte array is returned wrapped in {@link Optional}.
     * If no {@code EXIF} chunk exists, {@link Optional#empty()} is returned.
     * </p>
     *
     * @return an {@link Optional} containing the EXIF data as a byte array if found, or
     *         {@link Optional#empty()} if absent
     */
    public Optional<byte[]> getExifData()
    {
        for (WebpChunk chunk : chunks)
        {
            if (chunk.getType() == WebPChunkType.EXIF)
            {
                byte[] data = chunk.getPayloadArray();

                /*
                 * According to research on other sources, it seems sometimes the WebP files happen
                 * to contain the JPG premable within the TIFF header block for some strange
                 * reasons, the snippet below makes sure the JPEG segment is skipped.
                 */
                if (data.length >= JpgParser.JPG_EXIF_IDENTIFIER.length && Arrays.equals(Arrays.copyOf(data, JpgParser.JPG_EXIF_IDENTIFIER.length), JpgParser.JPG_EXIF_IDENTIFIER))
                {
                    data = Arrays.copyOfRange(data, JpgParser.JPG_EXIF_IDENTIFIER.length, data.length);
                }

                return Optional.of(data);
            }
        }

        return Optional.empty();
    }

    /**
     * Begins metadata processing by parsing the WebP file and extracting chunk data.
     *
     * @return true if at least one chunk element was successfully extracted, or false if no
     *         relevant data was processed
     *
     * @throws ImageReadErrorException
     *         if an error occurs while parsing the file
     * @throws IOException
     *         if there is a problem reading the file
     */
    @Override
    public boolean parseMetadata(Path imageFile) throws ImageReadErrorException, IOException
    {
        long fileLength = Files.size(imageFile);

        if (fileLength < fileSize)
        {
            throw new ImageReadErrorException("Declared file size exceeds actual file length.");
        }

        readChunk(imageFile);

        if (chunks.isEmpty())
        {
            LOGGER.info("No chunks extracted from WebP file [" + imageFile + "]");
            return false;
        }

        return true;
    }

    /**
     * Returns a textual representation of all parsed WebP chunks in this file.
     *
     * @return formatted string of all parsed chunk entries
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (WebpChunk chunk : chunks)
        {
            sb.append(chunk).append(System.lineSeparator());
        }

        return sb.toString();
    }

    /**
     * Adds a parsed chunk to the internal chunk collection.
     *
     * @param fourCC
     *        the 32-bit FourCC chunk identifier (in little-endian integer form)
     * @param length
     *        the length of the chunk's payload
     * @param data
     *        raw chunk data
     */
    private void addChunk(int fourCC, int length, byte[] data) throws ImageReadErrorException
    {
        WebPChunkType type = WebPChunkType.findType(fourCC);

        if (!type.isMultipleAllowed() && existsChunk(type))
        {
            LOGGER.warn("Duplicate chunk detected [" + type + "]");
        }

        chunks.add(new WebpChunk(fourCC, length, data));
    }

    /**
     * Processes the WebP data stream and extracts matching chunk types into memory.
     *
     * @throws ImageReadErrorException
     *         if invalid structure or erroneous chunks are detected
     */
    private void readChunk(Path imageFile) throws ImageReadErrorException
    {
        byte[] data;
        boolean firstChunk = true;

        do
        {
            int fourCC = reader.readInteger();
            int payloadLength = reader.readInteger();

            WebPChunkType chunkType = WebPChunkType.findType(fourCC);

            if (payloadLength < 0 || payloadLength > fileSize)
            {
                throw new ImageReadErrorException("Chunk Payload too large. Found [" + payloadLength + "]");
            }

            if (firstChunk && !FIRST_CHUNK_TYPES.contains(chunkType))
            {
                throw new ImageReadErrorException("First Chunk must be either VP8, VP8L, or VP8X. Found [" + WebPChunkType.getChunkName(fourCC) + "]");
            }

            if (requiredChunks == null || requiredChunks.contains(chunkType))
            {
                data = reader.readBytes(payloadLength);
                addChunk(fourCC, payloadLength, data);
                LOGGER.debug("Chunk type [" + chunkType + "] added for file [" + imageFile + "]");
            }

            else
            {
                reader.skip(payloadLength);
                LOGGER.debug("Chunk type [" + chunkType + "] skipped for file [" + imageFile + "]");
            }

            /*
             * According to the RIFF specification, any payload size having an
             * odd length must be added with one padding byte to make it even.
             */
            if (payloadLength % 2 != 0)
            {
                reader.skip(1);
            }

            firstChunk = false;

        } while (reader.getCurrentPosition() < fileSize);
    }

    /**
     * Read the file header of the given WebP file. Basically, it checks for correct RIFF and WEBP
     * signature entries within the first few stream bytes. It also determines the full size of this
     * file.
     *
     * @throws IllegalStateException
     *         if the WebP header information is corrupted
     */
    private static int readFileHeader(SequentialByteReader reader)
    {
        byte[] type = reader.readBytes(4);

        if (!Arrays.equals(RIFF.getChunkName().getBytes(StandardCharsets.US_ASCII), type))
        {
            throw new IllegalStateException("Header [RIFF] not found. Found [" + ByteValueConverter.toHex(type) + "]. Not a valid WEBP format");
        }

        int size = (int) reader.readUnsignedInteger() + 8;

        if (size < 0)
        {
            throw new IllegalStateException("WebP header contains a negative size. Found [" + size + "] bytes");
        }

        type = reader.readBytes(4);

        if (!Arrays.equals(WEBP.getChunkName().getBytes(), type))
        {
            throw new IllegalStateException("Chunk type [WEBP] not found. Found [" + ByteValueConverter.toHex(type) + "]. Not a valid WEBP format");
        }

        return size;
    }
}