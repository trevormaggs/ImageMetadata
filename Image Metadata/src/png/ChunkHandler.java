package png;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import common.DigitalSignature;
import common.ImageHandler;
import common.ImageReadErrorException;
import common.SequentialByteReader;
import logger.LogFactory;
import png.ChunkType.Category;

/**
 * Handles the processing and collection of metadata-related chunks from a PNG image file.
 *
 * <p>
 * This handler processes PNG chunks such as:
 * </p>
 *
 * <ul>
 * <li>{@code tEXt}, {@code iTXt}, {@code zTXt} – textual metadata chunks</li>
 * <li>{@code eXIf} – embedded EXIF metadata (in TIFF format)</li>
 * </ul>
 *
 * <p>
 * This class is typically used during PNG metadata parsing and delegates specific parsing
 * responsibilities to appropriate chunk or Exif handlers.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 21 June 2025
 */
public class ChunkHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(ChunkHandler.class);
    private final byte[] PNG_SIGNATURE_BYTES = DigitalSignature.PNG.getMagicNumbers(0);
    private static final boolean STRICTMODE = false;
    private final SequentialByteReader reader;
    private final EnumSet<ChunkType> requiredChunks;
    private final List<PngChunk> chunks;

    /**
     * This default constructor should not be used. It always throws an exception.
     *
     * @throws UnsupportedOperationException
     *         always thrown to prevent misuse
     */
    public ChunkHandler()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Constructs a handler to parse selected chunks from a PNG image file.
     *
     * @param reader
     *        byte reader for raw PNG stream
     * @param requiredChunks
     *        an optional set of chunk types to be extracted (null means all chunks are selected)
     */
    public ChunkHandler(SequentialByteReader reader, EnumSet<ChunkType> requiredChunks)
    {
        this.reader = reader;
        this.requiredChunks = requiredChunks;
        this.chunks = new ArrayList<>();
    }

    /**
     * Retrieves a list of chunks that have been extracted.
     *
     * @return an unmodified list of chunks
     */
    public List<PngChunk> getChunks()
    {
        return Collections.unmodifiableList(chunks);
    }

    /**
     * Retrieves all textual metadata chunks from the PNG file as an unmodifiable list.
     *
     * <p>
     * Textual metadata includes {@code tEXt}, {@code iTXt}, and {@code zTXt} chunks, which store
     * key-value text pairs or compressed textual information.
     * </p>
     *
     * @return an {@link Optional} containing a list of textual {@link PngChunk} objects if found,
     *         or {@link Optional#empty()} if no textual chunks are present
     */
    public Optional<List<PngChunk>> getTextualData()
    {
        List<PngChunk> textualChunks = new ArrayList<>();

        for (PngChunk chunk : chunks)
        {
            if (chunk.getType().getCategory() == Category.TEXTUAL)
            {
                textualChunks.add(chunk);
            }
        }

        return textualChunks.isEmpty() ? Optional.empty() : Optional.of(Collections.unmodifiableList(textualChunks));
    }

    /**
     * Retrieves the embedded EXIF data from the PNG file, if present.
     *
     * <p>
     * The EXIF metadata is stored in the {@code eXIf} chunk as raw TIFF-formatted data. If the PNG
     * file contains an {@code eXIf} chunk, its byte array is returned wrapped in {@link Optional}.
     * If no {@code eXIf} chunk exists, {@link Optional#empty()} is returned.
     * </p>
     *
     * @return an {@link Optional} containing the EXIF data as a byte array if found, or
     *         {@link Optional#empty()} if absent
     */
    public Optional<byte[]> getExifData()
    {
        for (PngChunk chunk : chunks)
        {
            if (chunk.getType() == ChunkType.eXIf)
            {
                return Optional.of(chunk.getPayloadArray());
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if a chunk with the specified type has already been set.
     *
     * @param type
     *        the type of the chunk
     *
     * @return true if the chunk is already present
     */
    public boolean existsChunk(ChunkType type)
    {
        return chunks.stream().anyMatch(chunk -> chunk.getType() == type);
    }

    /**
     * Begins metadata processing by parsing the PNG file and extracting chunk data.
     * 
     * It also checks if the PNG file contains the expected magic numbers in the first few bytes in
     * the file stream. If these numbers actually exist, they will then be skipped.
     *
     * @param imageFile
     *        the image file path
     * 
     * @return true if at least one chunk element was successfully extracted, or false if no
     *         relevant data was found
     *
     * @throws ImageReadErrorException
     *         if an error occurs while parsing the PNG file
     * @throws IOException
     *         if there is a problem reading the PNG file
     */
    @Override
    public boolean parseMetadata(Path imageFile) throws ImageReadErrorException, IOException
    {
        byte[] data = reader.peek(0, PNG_SIGNATURE_BYTES.length);

        /*
         * Note: PNG_SIGNATURE_BYTES (magic numbers) are mapped to
         * {0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}
         */
        if (data.length >= PNG_SIGNATURE_BYTES.length && Arrays.equals(Arrays.copyOf(data, PNG_SIGNATURE_BYTES.length), PNG_SIGNATURE_BYTES))
        {
            // Skip the actual PNG magic bytes safely
            reader.skip(PNG_SIGNATURE_BYTES.length);
        }

        readChunks(imageFile);

        if (chunks.isEmpty())
        {
            LOGGER.info("No chunks extracted from PNG file [" + imageFile + "]");
            return false;
        }

        return true;
    }

    /**
     * Returns a textual representation of all parsed PNG chunks in this file.
     *
     * @return formatted string of all parsed chunk entries
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        for (PngChunk chunk : chunks)
        {
            sb.append(chunk).append(System.lineSeparator());
        }

        return sb.toString();
    }

    /**
     * Processes the PNG data stream and extracts matching chunk types into memory.
     * 
     * @param imageFile
     *        the image file path
     *
     * @throws ImageReadErrorException
     *         if invalid structure or duplicate chunks are found, including a CRC calculation
     *         mismatch error
     */
    private void readChunks(Path imageFile) throws ImageReadErrorException
    {
        int position = 0;
        byte[] typeBytes;
        byte[] chunkData;
        ChunkType chunkType;

        do
        {
            chunkData = null;

            if (reader.getCurrentPosition() + 12 > reader.length())
            {
                /*
                 * 12 bytes = minimum chunk (length (4) + type (4) + CRC (4),
                 * even if data is zero-length)
                 */
                throw new ImageReadErrorException("Unexpected end of PNG file before IEND chunk detected");
            }

            int length = (int) reader.readUnsignedInteger();

            if (length < 0)
            {
                throw new ImageReadErrorException("Invalid PNG chunk length [" + length + "]");
            }

            typeBytes = reader.readBytes(4);
            chunkType = ChunkType.getChunkType(typeBytes);

            if (chunkType != ChunkType.UNKNOWN)
            {
                if (position == 0 && chunkType != ChunkType.IHDR)
                {
                    throw new ImageReadErrorException("PNG format error in file [" + imageFile + "]: First chunk must be [" + ChunkType.IHDR + "], but found [" + chunkType + "]");
                }

                if (!chunkType.isMultipleAllowed() && existsChunk(chunkType))
                {
                    throw new ImageReadErrorException("PNG format error in file [" + imageFile + "]: Duplicate [" + chunkType + "] found. This is disallowed");
                }

                if (requiredChunks == null || requiredChunks.contains(chunkType))
                {
                    chunkData = reader.readBytes(length);
                }

                else
                {
                    reader.skip(length);
                }

                int crc32 = (int) reader.readUnsignedInteger();

                if (chunkData != null)
                {
                    PngChunk newChunk = addChunk(chunkType, length, typeBytes, crc32, chunkData);

                    int expectedCrc = newChunk.calculateCrc();

                    if (expectedCrc != crc32)
                    {
                        String msg = String.format("CRC mismatch for chunk [%s] in file [%s]. Calculated: 0x%08X, Expected: 0x%08X. File may be corrupt.", chunkType, imageFile, expectedCrc, crc32);

                        if (STRICTMODE)
                        {
                            throw new ImageReadErrorException(msg);
                        }

                        else
                        {
                            LOGGER.warn(msg);
                        }
                    }

                    LOGGER.debug("Chunk type [" + chunkType + "] added for file [" + imageFile + "]");
                }
            }

            else
            {
                /* Skipped the full data length plus 4 bytes for CRC length */
                reader.skip(length + 4);
                LOGGER.warn("Unknown chunk type [" + chunkType + "] skipped");
            }

            position++;

        } while (chunkType != ChunkType.IEND);
    }

    /**
     * Adds a parsed chunk to the internal chunk collection. Special types such as {@code iTXt},
     * {@code zTXt}, and {@code tEXt} are instantiated into specific subclasses.
     *
     * @param chunkType
     *        the type of PNG chunk
     * @param length
     *        the data length of the chunk
     * @param typeBytes
     *        the raw 4-byte chunk type for CRC calculation
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     */
    private PngChunk addChunk(ChunkType chunkType, int length, byte[] typeBytes, int crc32, byte[] data) throws ImageReadErrorException
    {
        PngChunk newChunk;

        switch (chunkType)
        {
            case tEXt:
                newChunk = new PngChunkTEXT(length, typeBytes, crc32, data);
            break;

            case iTXt:
                newChunk = new PngChunkITXT(length, typeBytes, crc32, data);
            break;

            case zTXt:
                newChunk = new PngChunkZTXT(length, typeBytes, crc32, data);
            break;

            default:
                newChunk = new PngChunk(length, typeBytes, crc32, data);
        }

        chunks.add(newChunk);

        return newChunk;
    }
}