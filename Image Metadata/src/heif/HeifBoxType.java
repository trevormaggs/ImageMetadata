package heif;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Defines all supported HEIF (High Efficiency Image File) box types.
 *
 * <p>
 * Each {@code HeifBoxType} corresponds to a specific 4-character code used in the ISO Base Media
 * File Format (ISOBMFF) and HEIF/HEIC specifications.
 * </p>
 *
 * <p>
 * Boxes are categorized as:
 * </p>
 *
 * <ul>
 * <li>{@link BoxCategory#ATOMIC} – Individual data boxes containing fields</li>
 * <li>{@link BoxCategory#CONTAINER} – Structural boxes that contain child boxes</li>
 * <li>{@link BoxCategory#UNKNOWN} – Unknown or un-handled box types</li>
 * </ul>
 *
 * <p>
 * For official specifications, refer to:
 * </p>
 *
 * <ul>
 * <li>ISO/IEC 14496-12:2015 (ISOBMFF)</li>
 * <li>ISO/IEC 23008-12:2017 (HEIF)</li>
 * </ul>
 *
 * <p>
 * Use {@link #fromTypeName(String)} or {@link #fromTypeBytes(byte[])} to resolve box types at runtime.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public enum HeifBoxType
{
    UUID("uuid", BoxCategory.ATOMIC),
    FILE_TYPE("ftyp", BoxCategory.ATOMIC),
    PRIMARY_ITEM("pitm", BoxCategory.ATOMIC),
    ITEM_PROPERTY_ASSOCIATION("ipma", BoxCategory.ATOMIC),
    ITEM_PROTECTION("ipro", BoxCategory.ATOMIC),
    ITEM_DATA("idat", BoxCategory.ATOMIC),
    ITEM_INFO("iinf", BoxCategory.ATOMIC),
    ITEM_REFERENCE("iref", BoxCategory.CONTAINER),
    ITEM_LOCATION("iloc", BoxCategory.ATOMIC),
    HANDLER("hdlr", BoxCategory.ATOMIC),
    HVC1("hvc1", BoxCategory.ATOMIC),
    IMAGE_SPATIAL_EXTENTS("ispe", BoxCategory.ATOMIC),
    AUXILIARY_TYPE_PROPERTY("auxC", BoxCategory.ATOMIC),
    IMAGE_ROTATION("irot", BoxCategory.ATOMIC),
    COLOUR_INFO("colr", BoxCategory.ATOMIC),
    PIXEL_INFO("pixi", BoxCategory.ATOMIC),
    METADATA("meta", BoxCategory.CONTAINER),
    ITEM_PROPERTIES("iprp", BoxCategory.CONTAINER),
    ITEM_PROPERTY_CONTAINER("ipco", BoxCategory.CONTAINER),
    DATA_INFORMATION("dinf", BoxCategory.CONTAINER),
    MEDIA_DATA("mdat", BoxCategory.CONTAINER),
    UNKNOWN("unknown", BoxCategory.UNKNOWN);

    /**
     * Describes the general role of the box in the file structure.
     */
    public enum BoxCategory
    {
        /**
         * An individual box containing data fields.
         */
        ATOMIC,

        /**
         * A container box holding child boxes.
         */
        CONTAINER,

        /**
         * An unknown or unsupported box type.
         */
        UNKNOWN;
    }

    private final String typeName;
    private final BoxCategory category;
    private final byte[] typeBytes;
    private static final Map<String, HeifBoxType> NAME_LOOKUP = new HashMap<>();
    private static final Map<String, HeifBoxType> BYTES_LOOKUP = new HashMap<>();

    static
    {
        for (HeifBoxType type : values())
        {
            NAME_LOOKUP.put(type.typeName.toLowerCase(Locale.ROOT), type);
            BYTES_LOOKUP.put(new String(type.typeBytes, StandardCharsets.US_ASCII), type);
        }
    }

    /**
     * Constructs a {@code HeifBoxType} enum constant with its 4-character identifier and category.
     *
     * @param typeName
     *        the 4-character box type, for example, {@code ftyp}
     * @param category
     *        the box's structural category
     */
    private HeifBoxType(String typeName, BoxCategory category)
    {
        this.typeName = typeName;
        this.category = category;

        // Pre-calculate the US-ASCII byte representation of the type name
        this.typeBytes = typeName.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Returns the 4-character string identifier for this box type.
     *
     * @return the box type string, for example, {@code ftyp}, {@code meta}, {@code idat} etc
     */
    public String getTypeName()
    {
        return typeName;
    }

    /**
     * Returns the category of this box (ATOMIC, CONTAINER, or UNKNOWN).
     *
     * @return the box category
     */
    public BoxCategory getBoxCategory()
    {
        return category;
    }

    /**
     * Checks if the type of this box name matches the specified case-insensitive string. This
     * method is generally used for internal comparisons within the enum. For external lookup,
     * {@link #fromTypeName(String)} is recommended.
     *
     * @param name
     *        the box name to compare
     * 
     * @return true if the names match
     */
    public boolean equalsTypeName(String name)
    {
        return typeName.equalsIgnoreCase(name);
    }

    /**
     * Resolves a {@code HeifBoxType} from a 4-character string. This uses a pre-populated map for
     * efficient lookup, making it O(1) on average.
     *
     * @param name
     *        the box type name, for example, {@code ftyp}, {@code meta}, etc
     * 
     * @return the corresponding {@code HeifBoxType}, or {@link #UNKNOWN} if it is not recognised
     */
    public static HeifBoxType fromTypeName(String name)
    {
        if (name == null)
        {
            return UNKNOWN;
        }

        return NAME_LOOKUP.getOrDefault(name.toLowerCase(Locale.ROOT), UNKNOWN);
    }

    /**
     * Resolves a {@code HeifBoxType} from a 4-byte array. Comparison is performed using US-ASCII
     * encoding and a pre-populated map for efficiency. This method is O(1) on average after the
     * initial byte array to string conversion.
     *
     * @param raw
     *        the 4-byte box identifier
     * 
     * @return the corresponding {@code HeifBoxType}, or {@link #UNKNOWN} if it is not recognised
     */
    public static HeifBoxType fromTypeBytes(byte[] raw)
    {
        if (raw == null || raw.length != 4)
        {
            return UNKNOWN;
        }

        /*
         * Convert the input byte array to a String for map lookup. This is safe
         * because the keys in BYTES_LOOKUP are also created from US-ASCII bytes.
         */
        String key = new String(raw, StandardCharsets.US_ASCII);

        return BYTES_LOOKUP.getOrDefault(key, UNKNOWN);
    }
}