package webp;

import static webp.WebPChunkType.RIFF;
import static webp.WebPChunkType.VP8;
import static webp.WebPChunkType.VP8L;
import static webp.WebPChunkType.VP8X;
import static webp.WebPChunkType.WEBP;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import common.ByteValueConverter;
import common.DigitalSignature;
import common.ImageHandler;
import common.ImageReadErrorException;
import common.SequentialByteReader;
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
    private final Path imageFile;
    private final SequentialByteReader reader;
    private final List<WebpChunk> chunks;
    private final EnumSet<WebPChunkType> requiredChunks;
    private int fileSize;

    /**
     * This default constructor should not be used. It always throws an exception.
     *
     * @throws UnsupportedOperationException
     *         always thrown to prevent misuse
     */
    public WebpHandler()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Constructs a handler to parse selected chunks from a WebP image file.
     *
     * @param fpath
     *        the WebP file path
     * @param reader
     *        byte reader for raw WebP stream
     * @param requiredChunks
     *        an optional set of chunk types to be extracted (null means all chunks are selected)
     */
    public WebpHandler(Path fpath, SequentialByteReader reader, EnumSet<WebPChunkType> requiredChunks)
    {
        this.imageFile = fpath;
        this.reader = reader;
        this.chunks = new ArrayList<>();
        this.requiredChunks = requiredChunks;
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
    public boolean parseMetadata() throws ImageReadErrorException, IOException
    {
        verifySignature();
        readFileHeader();
        readChunk();

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
     * Checks if the WebP file contains the expected magic numbers in the first few bytes in the
     * stream data. If these numbers are correctly verified, no exception will be thrown.
     *
     * @throws ImageReadErrorException
     *         if the file contains incorrect magic numbers
     * @throws IOException
     *         if the magic numbers cannot be determined
     */
    private void verifySignature() throws ImageReadErrorException, IOException
    {
        if (DigitalSignature.detectFormat(imageFile) != DigitalSignature.WEBP)
        {
            throw new ImageReadErrorException("Invalid WebP signature detected in file [" + imageFile + "]");
        }
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
     *
     * @throws ImageReadErrorException
     *         if a duplicate chunk is found. Multiple chunks of the same type are not allowed
     */
    private void addChunk(int fourCC, int length, byte[] data) throws ImageReadErrorException
    {
        if (existsChunk(WebPChunkType.findType(fourCC)))
        {
            throw new ImageReadErrorException("Duplicate chunk detected [" + fourCC + "]");
        }

        System.out.printf("fourCC = %s\n", WebPChunkType.findType(fourCC));

        chunks.add(new WebpChunk(fourCC, length, data));
        LOGGER.debug("Chunk type [" + WebPChunkType.findType(fourCC) + "] added for file [" + imageFile + "]");
    }

    /**
     * Read the file header of the given WebP file. Basically, it checks for correct RIFF and WEBP
     * signature entries within the first few stream bytes. It also determines the full size of this
     * file.
     *
     * @throws ImageReadErrorException
     *         if the WebP header information is corrupted
     */
    private void readFileHeader() throws ImageReadErrorException
    {
        byte[] type = reader.readBytes(4);

        if (!Arrays.equals(RIFF.getChunkName().getBytes(), type))
        {
            throw new ImageReadErrorException("Header [RIFF] not found in file [" + imageFile + "]. Found [" + ByteValueConverter.toHex(type) + "]. Not a valid WEBP format");
        }

        fileSize = (int) reader.readUnsignedInteger();

        if (fileSize < 0)
        {
            throw new ImageReadErrorException("File [" + imageFile + "] has a negative size. Found [" + fileSize + "] bytes");
        }

        type = reader.readBytes(4);

        if (!Arrays.equals(WEBP.getChunkName().getBytes(), type))
        {
            throw new ImageReadErrorException("Chunk type [WEBP] not found in file [" + imageFile + "]. Found [" + ByteValueConverter.toHex(type) + "]. Not a valid WEBP format");
        }
    }

    /**
     * Processes the WebP data stream and extracts matching chunk types into memory.
     *
     * @throws ImageReadErrorException
     *         if invalid structure or erroneous chunks are detected
     */
    private void readChunk() throws ImageReadErrorException
    {
        byte[] data;
        boolean firstChunk = true;

        do
        {
            int fourCC = reader.readInteger();
            int payloadLength = reader.readInteger();

            WebPChunkType chunkType = WebPChunkType.findType(fourCC);

            if (payloadLength < 0)
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
                LOGGER.debug("Chunk type [" + chunkType + "] skipped");
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
}