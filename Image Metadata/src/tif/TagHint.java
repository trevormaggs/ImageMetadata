package tif;

public enum TagHint
{
    HINT_BYTE("8-bit byte"),
    HINT_STRING("Java String"),
    HINT_DATE("Java Date"),
    HINT_INTEGER("32-bit integer"),
    HINT_RATIONAL("2 unsigned LONGs"),
    HINT_UNDEFINED("Likely a byte array"),
    HINT_SHORT("16-bit short"),
    HINT_FLOAT("Single precision"),
    HINT_DOUBLE("Double precision"),
    HINT_DEFAULT("Default"),
    HINT_MASK("Masked string"),
    HINT_UNKNOWN("Hint is unknown");

    private final String description;

    private TagHint(String desc)
    {
        description = desc;
    }

    public String getDescription()
    {
        return description;
    }
}