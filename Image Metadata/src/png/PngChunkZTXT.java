package png;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;
import logger.LogFactory;
import common.ByteValueConverter;

/**
 * Extended to support a zTXt (compressed textual data) chunk in a PNG file.
 *
 * <p>
 * This class provides decoding support for PNG zTXt chunks, which store compressed Latin-1 text
 * paired with a keyword. The chunk format is:
 * </p>
 * 
 * <ul>
 * <li><b>Keyword</b>: 1–79 bytes (Latin-1), followed by a null byte</li>
 * <li><b>Compression method</b>: 1 byte (only value 0 is valid for zlib/deflate)</li>
 * <li><b>Compressed text</b>: remaining bytes, compressed using zlib</li>
 * </ul>
 *
 * @version 0.2
 * @since 21 June 2025
 */
public class PngChunkZTXT extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkZTXT.class);
    private String keyword;
    private String text;

    /**
     * Constructs a {@code PngChunkZTXT} instance.
     *
     * @param length
     *        the data length of the chunk (excluding chunk type and CRC)
     * @param chunkType
     *        the type of the chunk (should be {@code zTXt})
     * @param crc
     *        the CRC value (currently not validated)
     * @param data
     *        the raw byte array representing the chunk contents
     */
    public PngChunkZTXT(int length, ChunkType chunkType, int crc, byte[] data)
    {
        super(length, chunkType, crc, data);
    }

    /**
     * Extracts and de-compresses the keyword-text pair from this zTXt chunk.
     *
     * @return a {@link TextEntry} containing the keyword and the de-compressed text, or
     *         {@code null} if parsing fails
     * @throws IllegalStateException
     *         if the compression method is unsupported or decompression fails
     */
    @Override
    public TextEntry getKeywordPair()
    {
        byte[] data = getDataArray();

        try
        {
            // Read to length of keyword from offset 0
            keyword = new String(ByteValueConverter.trimNullTerminatedByteArray(data), StandardCharsets.ISO_8859_1);

            int pos = keyword.length() + 1;

            if (pos >= data.length)
            {
                throw new IllegalStateException("Malformed zTXt chunk: missing compression method or compressed text");
            }

            // Read one byte after length of keyword plus one null character
            int compressionMethod = data[pos++] & 0xFF;

            if (compressionMethod != 0)
            {
                throw new IllegalStateException("Invalid compression method in PNG zTXt chunk. Expected 0. Found: [" + compressionMethod + "]");
            }

            // Read full length of compressed text data after
            // compressionMethod's position without null terminator
            if (pos >= data.length)
            {
                throw new IllegalStateException("No compressed text found in PNG zTXt chunk");
            }

            // Extract compressed data
            byte[] rawCompressedText = Arrays.copyOfRange(data, pos, data.length);

            try (InputStream inflater = new InflaterInputStream(new ByteArrayInputStream(rawCompressedText)))
            {
                byte[] decompressed = ByteValueConverter.readAllBytes(inflater);
                text = new String(decompressed, StandardCharsets.ISO_8859_1);
            }

            return new TextEntry(getTag(), keyword, text);

        }
        catch (IOException | IllegalStateException exc)
        {
            LOGGER.error("Error parsing zTXt chunk: " + exc.getMessage());
        }

        return null;
    }

    /**
     * Gets the keyword extracted from the iTXt chunk.
     *
     * @return the keyword, or {@code null} if not yet parsed
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Gets the text extracted from the iTXt chunk.
     *
     * @return the UTF-8 text, or {@code null} if not yet parsed
     */
    public String getText()
    {
        return text;
    }
}