package png;

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
import common.DigitalSignature;
import common.ImageFileInputStream;
import common.ImageHandler;
import common.ImageReadErrorException;
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
 * @since 13 August 2025
 */
public class ChunkHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(ChunkHandler.class);
    private static final byte[] PNG_SIGNATURE_BYTES = DigitalSignature.PNG.getMagicNumbers(0);
    private final boolean strictMode;
    private final Path imageFile;
    private final EnumSet<ChunkType> requiredChunks;
    private final List<PngChunk> chunks;
    private final ImageFileInputStream reader;

    /**
     * Constructs a handler to parse selected chunks from a PNG image file, assuming the read mode
     * is not strict.
     *
     * @param fpath
     *        the path to the PNG file for logging purposes
     * @param reader
     *        byte reader for raw PNG stream
     * @param requiredChunks
     *        an optional set of chunk types to be extracted (null means all chunks are selected)
     */
    public ChunkHandler(Path fpath, ImageFileInputStream reader, EnumSet<ChunkType> requiredChunks)
    {
        this(fpath, reader, requiredChunks, false);
    }

    /**
     * Constructs a handler to parse selected chunks from a PNG image file.
     *
     * @param fpath
     *        the path to the PNG file for logging purposes
     * @param reader
     *        byte reader for raw PNG stream
     * @param requiredChunks
     *        an optional set of chunk types to be extracted (null means all chunks are selected)
     * @param strict
     *        true to make it strict, otherwise false for a lenient reading process
     */
    public ChunkHandler(Path fpath, ImageFileInputStream reader, EnumSet<ChunkType> requiredChunks, boolean strict)
    {
        this.imageFile = fpath;
        this.reader = reader;
        this.requiredChunks = requiredChunks;
        this.strictMode = strict;
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
     * 
     * @throws ImageReadErrorException
     *         if multiple chunks, that are disallowed, are detected
     */
    public Optional<byte[]> getExifData() throws ImageReadErrorException
    {
        byte[] data = null;

        for (PngChunk chunk : chunks)
        {
            if (chunk.getType() == ChunkType.eXIf)
            {
                if (data != null && strictMode)
                {
                    throw new ImageReadErrorException("Multiple eXIf chunks found. PNG is invalid");
                }

                data = chunk.getPayloadArray();
            }
        }

        return Optional.ofNullable(data);
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
     * Returns the size of the image file being processed, in bytes.
     * 
     * <p>
     * Any {@link IOException} that occurs while determining the size will be handled internally,
     * and the method will return {@code 0} if the size cannot be determined.
     * </p>
     *
     * @return the file size in bytes, or 0 if it cannot be determined
     */
    @Override
    public long getSafeFileSize()
    {
        try
        {
            return Files.size(imageFile);
        }

        catch (IOException exc)
        {
            return 0L;
        }
    }

    /**
     * Begins metadata processing by parsing the PNG file and extracting chunk data.
     *
     * It also checks if the PNG file contains the expected magic numbers in the first few bytes in
     * the file stream. If these numbers actually exist, they will then be skipped.
     *
     * @return true if at least one chunk element was successfully extracted, or false if no
     *         relevant data was found
     *
     * @throws ImageReadErrorException
     *         if an error occurs while parsing the PNG file
     * @throws IOException
     *         if there is an I/O stream error
     */
    @Override
    public boolean parseMetadata() throws IOException, ImageReadErrorException
    {
        byte[] signature = reader.readBytes(PNG_SIGNATURE_BYTES.length);

        if (signature.length < PNG_SIGNATURE_BYTES.length)
        {
            LOGGER.warn("Data is too short in file [" + imageFile + "]");
            return false;
        }

        /*
         * Note: PNG_SIGNATURE_BYTES (magic numbers) are mapped to
         * {0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}
         */
        if (!Arrays.equals(signature, PNG_SIGNATURE_BYTES))
        {
            throw new ImageReadErrorException("PNG file [" + imageFile + "] has an invalid signature. File may be corrupted.");
        }

        parseChunks();

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
     * @throws ImageReadErrorException
     *         if invalid structure or duplicate chunks are found, including a CRC calculation
     *         mismatch error
     * @throws IOException
     *         if there is an I/O stream error
     */
    private void parseChunks() throws ImageReadErrorException, IOException
    {
        int position = 0;
        byte[] typeBytes;
        ChunkType chunkType;
        long fileSize = getSafeFileSize();

        do
        {
            if (fileSize == 0 || reader.getCurrentPosition() + 12 > fileSize)
            {
                /*
                 * 12 bytes = minimum chunk (length (4) + type (4) + CRC (4),
                 * even if data is zero-length)
                 */
                throw new ImageReadErrorException("Unexpected end of PNG file before IEND chunk detected");
            }

            long length = reader.readUnsignedInteger();

            if (length < 0 || length > Integer.MAX_VALUE)
            {
                throw new ImageReadErrorException("Out of bounds chunk length [" + length + "] detected");
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

                byte[] chunkData = (requiredChunks == null || requiredChunks.contains(chunkType) ? reader.readBytes((int) length) : null);

                if (chunkData == null)
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

                        if (strictMode)
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
                LOGGER.warn("Unknown chunk type [" + new String(typeBytes, StandardCharsets.US_ASCII) + "] skipped");
                LOGGER.debug("Data skipped by length [" + (length + 4) + "] in file [" + imageFile + "] due to an unknown chunk");
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
    private PngChunk addChunk(ChunkType chunkType, long length, byte[] typeBytes, int crc32, byte[] data)
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