package png;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
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
     *        the length of the chunk's data field (excluding type and CRC)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     */
    public PngChunkTEXT(int length, byte[] typeBytes, int crc, byte[] data)
    {
        super(length, typeBytes, crc, data);
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
     * <p>
     * <strong>API Note:</strong> this {@link getKeywordPair()} method must be invoked first before
     * calling other getter methods.
     * </p>
     *
     * @return an {@link Optional} containing the extracted keyword and text as a {@link TextEntry}
     *         instance if present, otherwise, {@link Optional#empty()}
     */
    @Override
    public Optional<TextEntry> getKeywordPair()
    {
        byte[] data = getDataArray();
        String[] parts = ByteValueConverter.splitNullDelimitedStrings(data, StandardCharsets.ISO_8859_1);

        if (parts.length < 2)
        {
            LOGGER.warn("tEXt chunk missing null separator or malformed [" + Arrays.toString(data) + "]");
            return Optional.empty();
        }

        keyword = parts[0];
        text = parts[1];

        if (keyword.length() == 0 || keyword.length() > 79)
        {
            LOGGER.warn("Invalid tEXt keyword length (must be 1–79 characters)");
            return Optional.empty();
        }

        return Optional.of(new TextEntry(getTag(), keyword, text));
    }

    /**
     * Returns the decoded keyword from this tEXt chunk. <b>Note</b>, the {@link #getKeywordPair()}
     * method must be called first before calling this method to ensure data integrity.
     *
     *
     * @return the text or null if not yet decoded
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Returns the decoded text from this tEXt chunk. <b>Note</b>, the {@link #getKeywordPair()}
     * method must be called first before calling this method to ensure data integrity.
     *
     *
     * @return the text or null if not yet decoded
     */
    public String getText()
    {
        return text;
    }

    /**
     * Returns a string representation of the chunk's properties and contents.
     *
     * @return a formatted string describing this chunk
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(super.toString());
        line.append(String.format(" %-20s %s%n", "[Keyword]", getKeyword()));
        line.append(String.format(" %-20s %s%n", "[Text]", getText()));

        return line.toString();
    }
}