package png;

import tif.TagEntries.TagPngChunk;

/**
 * Represents a textual metadata entry within a PNG chunk, consisting of a tag type,
 * keyword, and associated text value.
 *
 * Typically used to store entries from textual chunks such as {@code tEXt}, {@code iTXt}, or
 * {@code zTXt}.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class TextEntry
{
    private final TagPngChunk tag;
    private final String keyword;
    private final String text;

    /**
     * Constructs a {@code TextEntry} with a PNG tag, keyword, and associated text value.
     *
     * @param tag
     *        the tag indicating the PNG chunk type
     * @param keyword
     *        the keyword name identifying the entry
     * @param text
     *        the text value associated with the keyword
     */
    public TextEntry(TagPngChunk tag, String keyword, String text)
    {
        this.tag = tag;
        this.keyword = keyword;
        this.text = text;
    }

    /**
     * Returns the tag associated with this entry.
     *
     * @return the {@link TagPngChunk} type
     */
    public TagPngChunk getTag()
    {
        return tag;
    }

    /**
     * Returns the keyword for this entry.
     *
     * @return the keyword string
     */
    public String getKeyword()
    {
        return keyword;
    }

    public TextKeyword getKeywordEnum()
    {
        return TextKeyword.getKeyword(keyword);
    }

    /**
     * Returns the interpreted value of this text entry.
     *
     * @return the raw or parsed value string
     */
    public String getValue()
    {
        return text;
    }

    /**
     * Returns a formatted string representation of this text entry.
     *
     * @return a detailed multi-line string
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(String.format(" %-20s %s%n", "[Tag Type]", tag));
        line.append(String.format(" %-20s %s%n", "[Keyword]", keyword));
        line.append(String.format(" %-20s %s%n", "[Text]", text));

        return line.toString();
    }
}