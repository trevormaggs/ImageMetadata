package png;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import common.ByteValueConverter;
import logger.LogFactory;

/**
 * Represents a {@code tEXt} chunk in a PNG file, which stores original textual data.
 *
 * This chunk contains a keyword and associated text string, both encoded in Latin-1. It extends
 * {@link PngChunk} to provide decoding of the textual content into a {@link TextEntry}.
 *
 * @version 1.0
 * @since 28 July 2025
 */
public class PngChunkTEXT extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkTEXT.class);
    private final String keyword;
    private final String text;

    /**
     * Constructs a {@code PngChunkTEXT} instance with the specified metadata.
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
     * @param length
     *        the length of the chunk's data field (excluding type and CRC)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     */
    public PngChunkTEXT(int length, byte[] typeBytes, int crc32, byte[] data)
    {
        super(length, typeBytes, crc32, data);

        String[] parts = ByteValueConverter.splitNullDelimitedStrings(payload, StandardCharsets.ISO_8859_1);

        if (parts.length < 2)
        {
            LOGGER.warn("tEXt chunk missing null separator or malformed [" + ByteValueConverter.toHex(payload) + "]");

            this.keyword = "";
            this.text = "";

            return;
        }

        String parsedKeyword = parts[0];
        String parsedText = parts[1];

        if (parsedKeyword.length() == 0 || parsedKeyword.length() > 79)
        {
            LOGGER.warn("Invalid tEXt keyword length (must be 1–79 characters). Keyword found [" + parsedKeyword + "]");

            this.keyword = "";
            this.text = "";

            return;
        }

        this.keyword = parsedKeyword;
        this.text = parsedText;
    }

    /**
     * Checks whether this chunk contains the specified textual keyword.
     *
     * @param keyword
     *        the {@link TextKeyword} to search for
     *
     * @return true if found, false otherwise
     *
     * @throws IllegalArgumentException
     *         if the specified keyword is null
     */
    @Override
    public boolean hasKeywordPair(TextKeyword keyword)
    {
        if (keyword == null || keyword.getKeyword() == null)
        {
            throw new IllegalArgumentException("Keyword cannot be null");
        }

        return keyword.getKeyword().equals(this.keyword);
    }

    /**
     * Extracts the keyword-text pair from the {@code tEXt} chunk.
     *
     * @return an {@link Optional} containing the extracted keyword and text as a {@link TextEntry}
     *         instance if present, otherwise, {@link Optional#empty()}
     */
    @Override
    public Optional<TextEntry> getKeywordPair()
    {
        if (keyword.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.of(new TextEntry(getTag(), keyword, text));
    }

    /**
     * Returns the decoded keyword from this tEXt chunk.
     *
     * @return the text or null if not yet decoded
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Returns the decoded text from this tEXt chunk.
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