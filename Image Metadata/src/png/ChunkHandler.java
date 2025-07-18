package png;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import common.BaseMetadata;
import common.ImageHandler;
import common.ImageReadErrorException;
import common.Metadata;
import common.SequentialByteReader;
import logger.LogFactory;
import png.ChunkType.Category;
import tif.TifParser;

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
    private final Path imageFile;
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
     * @param fpath
     *        the PNG file path
     * @param reader
     *        byte reader for raw PNG stream
     * @param requiredChunks
     *        an optional set of chunk types to be extracted (null means all chunks are selected)
     */
    public ChunkHandler(Path fpath, SequentialByteReader reader, EnumSet<ChunkType> requiredChunks)
    {
        this.imageFile = fpath;
        this.reader = reader;
        this.requiredChunks = requiredChunks;
        this.chunks = new ArrayList<>();
    }

    /**
     * Reads the PNG data stream and extracts matching chunk types into memory.
     *
     * @throws ImageReadErrorException
     *         if invalid structure or duplicate chunks are found
     * @throws IOException
     *         if I/O issues occur while reading
     */
    private void readChunks() throws ImageReadErrorException, IOException
    {
        int position = 0;
        ChunkType chunkType;

        do
        {
            int length = (int) reader.readUnsignedInteger();

            if (length < 0)
            {
                throw new ImageReadErrorException("Invalid PNG chunk length [" + length + "]");
            }

            chunkType = ChunkType.getChunkType(reader.readBytes(4));

            if (!chunkType.isMultipleAllowed() && existsChunk(chunkType.getIndexID()))
            {
                throw new ImageReadErrorException("Multiple chunks of type [" + chunkType + "] are disallowed. PNG file may be corrupted.");
            }

            boolean isDesired = (requiredChunks == null || requiredChunks.contains(chunkType));
            byte[] chunkData = isDesired ? reader.readBytes(length) : null;

            if (!isDesired)
            {
                reader.skip(length);
            }

            /* We are not interested in CRC at this stage */
            // int crc = (int) reader.readUnsignedInteger();
            reader.skip(4);

            if (chunkType == ChunkType.IHDR && position > 0)
            {
                throw new ImageReadErrorException(String.format("First chunk must be [%s], but found [%s]", ChunkType.IHDR, chunkType));
            }

            if (isDesired && chunkData != null)
            {
                addChunk(length, chunkType, 0, chunkData);
                LOGGER.debug("Chunk type [" + chunkType + "] added for file [" + imageFile + "]");
            }

            position++;

        } while (chunkType != ChunkType.IEND);
    }

    private void readChunks2() throws ImageReadErrorException, IOException
    {
        int position = 0;
        ChunkType chunkType;
        Optional<byte[]> optionalData;

        do
        {
            int length = (int) reader.readUnsignedInteger();

            if (length < 0)
            {
                throw new ImageReadErrorException("Invalid PNG chunk length [" + length + "]");
            }

            chunkType = ChunkType.getChunkType(reader.readBytes(4));

            if (chunkType == ChunkType.IHDR && position > 0)
            {
                throw new ImageReadErrorException("First chunk must be [" + ChunkType.IHDR + "], but found [" + chunkType + "]");
            }

            if (!chunkType.isMultipleAllowed() && existsChunk(chunkType.getIndexID()))
            {
                throw new ImageReadErrorException("Multiple chunks of type [" + chunkType + "] are disallowed. PNG file may be corrupted");
            }

            if (requiredChunks == null || requiredChunks.contains(chunkType))
            {
                optionalData = Optional.of(reader.readBytes(length));
            }

            else
            {
                reader.skip(length);
                optionalData = Optional.empty();
            }

            /* We are not interested in CRC at this stage */
            // int crc = (int) reader.readUnsignedInteger();
            reader.skip(4);

            if (optionalData.isPresent())
            {
                addChunk(length, chunkType, 0, optionalData.get());

                LOGGER.debug("Chunk type [" + chunkType + "] added for file [" + imageFile + "]");
            }

            position++;

        } while (chunkType != ChunkType.IEND);
    }

    /**
     * Adds a parsed chunk to the internal chunk collection. Special types such as {@code iTXt},
     * {@code zTXt}, and {@code tEXt} are instantiated into specific subclasses.
     *
     * @param length
     *        the data length of the chunk
     * @param chunkType
     *        the type of PNG chunk
     * @param crc
     *        the CRC value (currently not implemented)
     * @param data
     *        raw chunk data
     * 
     * @throws IOException
     *         if processing fails
     */
    private void addChunk(int length, ChunkType chunkType, int crc, byte[] data) throws IOException
    {
        switch (chunkType)
        {
            case tEXt:
                chunks.add(new PngChunkTEXT(length, chunkType, crc, data));
            break;

            case iTXt:
                chunks.add(new PngChunkITXT(length, chunkType, crc, data));
            break;

            case zTXt:
                chunks.add(new PngChunkZTXT(length, chunkType, crc, data));
            break;

            default:
                chunks.add(new PngChunk(length, chunkType, crc, data));
        }
    }

    /**
     * Checks if a chunk with the given ID already exists in the parsed chunk list.
     *
     * @param chunkID
     *        the unique index ID of a chunk
     * @return true if the chunk is already present
     */
    private boolean existsChunk(int chunkID)
    {
        return chunks.stream().anyMatch(chunk -> chunk.getType().getIndexID() == chunkID);
    }

    /**
     * Begins metadata processing by scanning the PNG file and extracting chunk data. EXIF data
     * (from {@code eXIf}) is parsed using {@link TifParser}, and textual chunks are collected into
     * a {@link ChunkDirectory}.
     *
     * @return a {@link Metadata} object representing extracted PNG metadata
     * 
     * @throws ImageReadErrorException
     *         if an error occurs while parsing the PNG file
     * @throws IOException
     *         if the file is not in PNG format
     */
    @Override
    public Metadata<? extends BaseMetadata> processMetadata() throws IOException, ImageReadErrorException
    {
        readChunks();

        boolean exifFound = false;
        MetadataPNG<BaseMetadata> png = new MetadataPNG<>();
        ChunkDirectory textualDir = new ChunkDirectory(Category.TEXTUAL);

        for (PngChunk chunk : chunks)
        {
            ChunkType type = chunk.getType();

            if (type.getCategory() == Category.TEXTUAL)
            {
                textualDir.add(chunk);
            }

            else if (type == ChunkType.eXIf)
            {
                if (exifFound)
                {
                    LOGGER.error("Duplicate eXIf chunk detected in file [" + imageFile + "]");
                    throw new ImageReadErrorException("File [" + imageFile + "] contains duplicate eXIf chunks");
                }

                exifFound = true;
                png.addDirectory(new TifParser(imageFile, chunk.getDataArray()).getMetadata());
            }
        }

        png.addDirectory(textualDir);

        return png;
    }

    public void processMetadata2() throws ImageReadErrorException, IOException
    {
        readChunks();
    }

    // public Optional<byte[]> getExifData() throws ImageReadErrorException
    public byte[] getExifData() throws ImageReadErrorException
    {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            for (PngChunk chunk : chunks)
            {
                if (chunk.getType() == ChunkType.eXIf)
                {
                    baos.write(chunk.getDataArray());

                    // return Optional.of(baos.toByteArray());
                    return baos.toByteArray();
                }
            }

            // return Optional.empty();
            return new byte[0];

        }

        catch (IOException exc)
        {
            throw new ImageReadErrorException("Unable to process Exif block: [" + exc.getMessage() + "]", exc);
        }
    }

    public List<PngChunk> getTextualData() throws ImageReadErrorException
    {
        List<PngChunk> textualChunks = new ArrayList<>();

        for (PngChunk chunk : chunks)
        {
            if (chunk.getType().getCategory() == Category.TEXTUAL)
            {
                textualChunks.add(chunk);
            }
        }

        return textualChunks;
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
}