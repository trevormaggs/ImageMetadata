package heif;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Defines all supported HEIF (High Efficiency Image File) box types.
 * 
 * <p>
 * Each {@code HeifBoxType} corresponds to a specific 4-character code used in the ISO Base Media
 * File Format (ISOBMFF) and HEIF/HEIC specifications.
 * </p>
 * 
 * <p>
 * Boxes are categorised as:
 * </p>
 * 
 * <ul>
 * <li>{@link BoxCategory#BOX} – Atomic data boxes containing fields</li>
 * <li>{@link BoxCategory#CONTAINER} – Structural boxes that contain child boxes</li>
 * <li>{@link BoxCategory#UNDEFINED} – Unknown or unhandled box types</li>
 * </ul>
 * 
 * <p>
 * For official details, refer to:
 * </p>
 * 
 * <ul>
 * <li>ISO/IEC 14496-12:2015 (ISOBMFF)</li>
 * <li>ISO/IEC 23008-12:2017 (HEIF)</li>
 * </ul>
 * 
 * <p>
 * Use {@link #getBoxType(String)} or {@link #getBoxType(byte[])} to resolve box types at runtime.
 * </p>
 * 
 * @author Trevor Maggs
 * @since 17 June 2025
 */
public enum HeifBoxType
{
    UUID("uuid", BoxCategory.BOX),
    BOX_FILE_TYPE("ftyp", BoxCategory.BOX),
    BOX_PRIMARY_ITEM("pitm", BoxCategory.BOX),
    BOX_ITEM_PROPERTY_ASSOCIATION("ipma", BoxCategory.BOX),
    BOX_ITEM_PROTECTION("ipro", BoxCategory.BOX),
    BOX_ITEM_DATA("idat", BoxCategory.BOX),
    BOX_ITEM_INFO("iinf", BoxCategory.BOX),
    BOX_ITEM_REFERENCE("iref", BoxCategory.CONTAINER),
    BOX_ITEM_LOCATION("iloc", BoxCategory.BOX),
    BOX_HANDLER("hdlr", BoxCategory.BOX),
    BOX_HVC1("hvc1", BoxCategory.BOX),
    BOX_IMAGE_SPATIAL_EXTENTS("ispe", BoxCategory.BOX),
    BOX_AUXILIARY_TYPE_PROPERTY("auxC", BoxCategory.BOX),
    BOX_IMAGE_ROTATION("irot", BoxCategory.BOX),
    BOX_COLOUR_INFO("colr", BoxCategory.BOX),
    BOX_PIXEL_INFORMATION("pixi", BoxCategory.BOX),
    BOX_METADATA("meta", BoxCategory.CONTAINER),
    BOX_IMAGE_PROPERTY("iprp", BoxCategory.CONTAINER),
    BOX_ITEM_PROPERTY("ipco", BoxCategory.CONTAINER),
    BOX_DATA_INFORMATION("dinf", BoxCategory.CONTAINER),
    BOX_MEDIA_DATA("mdat", BoxCategory.CONTAINER),
    UNKNOWN("unknown", BoxCategory.UNDEFINED);

    /**
     * Describes the general role of the box in the file structure.
     */
    public enum BoxCategory
    {
        /**
         * A leaf box containing data fields.
         */
        BOX,

        /**
         * A container box holding child boxes.
         */
        CONTAINER,

        /**
         * An unrecognized or unsupported box type.
         */
        UNDEFINED;
    }

    private final String boxname;
    private final BoxCategory category;

    /**
     * Constructs a {@code HeifBoxType} resource with a 4-character identifier and category.
     *
     * @param name
     *        the 4-character box type (For example, {@code ftyp"}
     * @param category
     *        the box's structural category
     */
    private HeifBoxType(String name, BoxCategory category)
    {
        this.boxname = name;
        this.category = category;
    }

    /**
     * Returns the 4-character string identifier for this box type.
     *
     * @return the box type string (For example, "ftyp", "meta", "idat")
     */
    public String getTypeName()
    {
        return boxname;
    }

    /**
     * Returns the category of this box (BOX, CONTAINER, or UNDEFINED).
     *
     * @return the box category
     */
    public BoxCategory getBoxCategory()
    {
        return category;
    }

    /**
     * Checks if this box type is equal to another {@code HeifBoxType}.
     *
     * @param type
     *        the type to compare
     * 
     * @return true if they are the same type
     */
    public boolean equalsType(HeifBoxType type)
    {
        return this == type;
    }

    /**
     * Checks if this box's name matches the provided string (case-insensitive).
     *
     * @param name
     *        the box name to compare
     * 
     * @return true if the names match
     */
    public boolean matchesBoxName(String name)
    {
        return boxname.equalsIgnoreCase(name);
    }

    /**
     * Resolves a {@code HeifBoxType} from a 4-character string.
     *
     * @param name
     *        the box type name (For example, "meta", "ftyp")
     * 
     * @return the corresponding {@code HeifBoxType}, or {@link #UNKNOWN} if not recognised
     */
    public static HeifBoxType getBoxType(String name)
    {
        for (HeifBoxType type : values())
        {
            if (type.matchesBoxName(name))
            {
                return type;
            }
        }

        return UNKNOWN;
    }

    /**
     * Resolves a {@code HeifBoxType} from a 4-byte array.
     *
     * <p>
     * Comparison is performed using ASCII encoding.
     * </p>
     *
     * @param raw
     *        the 4-byte box identifier
     * 
     * @return the corresponding {@code HeifBoxType}, or {@link #UNKNOWN} if not recognised
     */
    public static HeifBoxType getBoxType(byte[] raw)
    {
        for (HeifBoxType type : values())
        {
            if (Arrays.equals(type.boxname.getBytes(StandardCharsets.US_ASCII), raw))
            {
                return type;
            }
        }

        return UNKNOWN;
    }
}