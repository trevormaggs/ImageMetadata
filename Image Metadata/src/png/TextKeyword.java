package png;

import tif.TagHint;

/**
 * Enumerates known PNG textual metadata keywords, typically found in chunks of type
 * {@code tEXt}, {@code iTXt}, or {@code zTXt}.
 *
 * Each entry maps a human-readable keyword and an optional {@link TagHint}, indicating how the
 * keyword value should be interpreted.
 *
 * <p>
 * Examples include: {@code Title}, {@code Author}, {@code Creation Time}, etc.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public enum TextKeyword
{
    TITLE("Title"),
    AUTHOR("Author"),
    DESC("Description"),
    COPYRIGHT("Copyright"),
    CREATE("Creation Time", TagHint.HINT_DATE),
    SOFTWARE("Software"),
    DISCLAIMER("Disclaimer"),
    WARNING("Warning"),
    SOURCE("Source"),
    COMMENT("Comment"),
    XML("XML:com.adobe.xmp"),
    COLLECTION("Collection"),
    OTHER("Unknown");

    private final String keyword;
    private final TagHint hint;

    /**
     * Constructs a {@code TextKeyword} with a keyword and a default hint of {@code HINT_STRING}.
     *
     * @param name
     *        the keyword label
     */
    private TextKeyword(String name)
    {
        this(name, TagHint.HINT_STRING);
    }

    /**
     * Constructs a {@code TextKeyword} with a keyword and an associated tag hint.
     *
     * @param name
     *        the keyword label
     * @param hint
     *        hint describing how the associated value should be interpreted
     */
    private TextKeyword(String name, TagHint hint)
    {
        this.keyword = name;
        this.hint = hint;
    }

    /**
     * Returns the string keyword associated with this enumeration constant.
     *
     * @return the textual keyword
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Returns the metadata interpretation hint associated with this keyword.
     *
     * @return the {@link TagHint} for this keyword
     */
    public TagHint getHint()
    {
        return hint;
    }

    /**
     * Attempts to resolve a {@code TextKeyword} enumeration from the specified string.
     *
     * @param word
     *        the keyword string to look up
     * 
     * @return the corresponding {@code TextKeyword}, or {@code OTHER} if unknown
     */
    public static TextKeyword getKeyword(String word)
    {
        for (TextKeyword keyword : values())
        {
            if (keyword.keyword.equalsIgnoreCase(word))
            {
                return keyword;
            }
        }

        return OTHER;
    }
}