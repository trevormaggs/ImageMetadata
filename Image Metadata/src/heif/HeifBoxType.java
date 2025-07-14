package heif;

import java.util.Arrays;

public enum HeifBoxType
{
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
    UNKNOWN("Unhandled box", BoxCategory.UNDEFINED);

    public enum BoxCategory
    {
        BOX,
        CONTAINER,
        UNDEFINED;
    }

    private final String boxname;
    private final BoxCategory category;

    private HeifBoxType(String name, BoxCategory category)
    {
        this.boxname = name;
        this.category = category;
    }

    public String getBoxName()
    {
        return boxname;
    }

    public BoxCategory getBoxCategory()
    {
        return category;
    }

    public boolean isEqualBox(HeifBoxType type)
    {
        return this == type;
    }

    public boolean isEqualBox(String name)
    {
        return boxname.equalsIgnoreCase(name);
    }

    public static HeifBoxType getBoxType(String name)
    {
        for (HeifBoxType type : values())
        {
            if (type.isEqualBox(name))
            {
                return type;
            }
        }

        return UNKNOWN;
    }

    public static HeifBoxType getBoxType(byte[] raw)
    {
        for (HeifBoxType type : values())
        {
            if (Arrays.equals(type.boxname.getBytes(), raw))
            {
                return type;
            }
        }

        return UNKNOWN;
    }
}