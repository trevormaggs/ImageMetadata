package png;

import java.nio.charset.StandardCharsets;
import logger.LogFactory;
import common.ByteValueConverter;

/**
 * Represents a {@code tEXt} chunk in a PNG file, which stores original textual data.
 * 
 * This chunk contains a keyword and associated text string, both encoded in Latin-1. It extends
 * {@link PngChunk} to provide decoding of the textual content into a {@link TextEntry}.
 *
 * @version 1.0
 * @since 21 June 2025
 */
public class PngChunkTEXT extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkTEXT.class);
    private String keyword;
    private String text;

    /**
     * Constructs a {@code PngChunkTEXT} instance with the specified metadata.
     *
     * @param length
     *        the length of the data payload (excluding type and CRC fields)
     * @param chunkType
     *        the chunk type as a {@link ChunkType}
     * @param crc
     *        the CRC value (not currently validated)
     * @param data
     *        the raw chunk data
     */
    public PngChunkTEXT(int length, ChunkType chunkType, int crc, byte[] data)
    {
        super(length, chunkType, crc, data);
    }

    /**
     * Extracts the keyword-text pair from the {@code tEXt} chunk.
     * 
     * <p>
     * According to the PNG specification:
     * </p>
     * 
     * <ul>
     * <li><strong>Keyword</strong>: Latin-1 string (1–79 bytes)</li>
     * <li><strong>Null Separator</strong>: 1 byte</li>
     * <li><strong>Text</strong>: Latin-1 string (0 or more bytes)</li>
     * </ul>
     *
     * @return a {@link TextEntry} containing the keyword and text, or {@code null} if invalid
     */
    @Override
    public TextEntry getKeywordPair()
    {
        byte[] data = getDataArray();
        String[] parts = ByteValueConverter.splitNullDelimitedStrings(data, StandardCharsets.ISO_8859_1);

        if (parts.length == 2)
        {
            this.keyword = parts[0];
            this.text = parts[1];
            return new TextEntry(getTag(), keyword, text);
        }

        else
        {
            LOGGER.error("Invalid tEXt chunk: expected keyword and text pair, but found " + parts.length + " parts.");
            return null;
        }
    }

    /**
     * Returns the decoded keyword from this tEXt chunk.
     *
     * @return the keyword, or {@code null} if not yet decoded
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Returns the decoded text from this tEXt chunk.
     *
     * @return the text, or {@code null} if not yet decoded
     */
    public String getText()
    {
        return text;
    }
}