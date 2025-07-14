package common;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import tif.DirectoryIFD.EntryIFD;
import tif.TagHint;

/**
 * Utility class for converting EntryIFD values to human-readable or numeric forms, using hint-aware
 * transformation rules.
 *
 * @version 1.0
 * @since 10 July 2025
 * @author Trevor Maggs
 */
public final class TagValueConverter
{
    /**
     * Default constructor is unsupported and will always throw an exception.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    private TagValueConverter()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Converts the value of an IFD entry into a string, applying any hint-based interpretation.
     *
     * @param entry
     *        the EntryIFD object
     * @return the string representation of the entry’s value
     */
    public static String toStringValue(EntryIFD entry)
    {
        Object obj = entry.getData();
        TagHint hint = entry.getTag().getHint();

        if (obj != null)
        {
            if (obj instanceof Byte || obj instanceof Number)
            {
                return obj.toString();
            }

            if (obj instanceof String)
            {
                if (hint == TagHint.HINT_DATE)
                {
                    return DateParser.convertToDate((String) obj).toString();
                }

                return ((String) obj).trim();
            }

            if (obj instanceof RationalNumber)
            {
                return ((RationalNumber) obj).toSimpleString(true);
            }

            if (obj instanceof byte[])
            {
                byte[] bytes = (byte[]) obj;

                switch (hint)
                {
                    case HINT_STRING:
                        return new String(ByteValueConverter.trimNullTerminatedByteArray(bytes), StandardCharsets.UTF_8);

                    case HINT_BYTE:
                        StringBuilder sb = new StringBuilder();

                        for (byte b : bytes)
                        {
                            sb.append(b);
                        }

                        return sb.toString();

                    case HINT_MASK:
                        return "[Masked items]";

                    default:
                        return Arrays.toString(bytes);
                }
            }

            return obj.toString();
        }

        return "";
    }

    /**
     * Converts the value of an IFD entry into a numeric form, if possible.
     *
     * @param entry
     *        the EntryIFD object
     * @return the numeric value as a Number, or 0 if not applicable
     */
    public static Number toNumericValue(EntryIFD entry)
    {
        Object obj = entry.getData();

        if (obj instanceof Number)
        {
            return (Number) obj;
        }

        return 0;
    }
}