package heif;

import java.nio.charset.StandardCharsets;
import common.SequentialByteReader;
import heif.boxes.*;

public final class BoxFactory
{
    public static Box createBox(SequentialByteReader reader)
    {
        Box box = new Box(reader);

        switch (HeifBoxType.fromTypeName(box.getTypeAsString()))
        {
            case FILE_TYPE:
                return new FileTypeBox(box, reader);
            case METADATA:
                return new MetaBox(box, reader);
            case HANDLER:
                return new HandlerBox(box, reader);
            case DATA_INFORMATION:
                return new DataInformationBox(box, reader);
            case PRIMARY_ITEM:
                return new PrimaryItemBox(box, reader);
            case ITEM_INFO:
                return new ItemInformationBox(box, reader);
            case ITEM_REFERENCE:
                return new ItemReferenceBox(box, reader);
            case ITEM_PROPERTIES:
                return new ItemPropertiesBox(box, reader);
            case COLOUR_INFO:
                return new ColourInformationBox(box, reader);
            case IMAGE_SPATIAL_EXTENTS:
                return new ImageSpatialExtentsProperty(box, reader);
            case IMAGE_ROTATION:
                return new ImageRotationBox(box, reader);
            case PIXEL_INFO:
                return new PixelInformationBox(box, reader);
            case AUXILIARY_TYPE_PROPERTY:
                return new AuxiliaryTypePropertyBox(box, reader);
            case ITEM_DATA:
                return new ItemDataBox(box, reader);
            case ITEM_LOCATION:
                return new ItemLocationBox(box, reader);

            /*
             * case BOX_ITEM_PROTECTION:
             * return new ItemProtectionBox(box, reader);
             * //case BOX_ITEM_PROPERTY_ASSOCIATION:
             * //return new ItemPropertyAssociation(box, reader);
             * 
             * case pasp:
             * return PixelAspectRatioBox(box, reader);
             * break;
             * 
             * case grpl:
             * break;
             * case rloc:
             * box = new RelativeLocationProperty(box, reader);
             * break;
             * case clap:
             * box = new CleanApertureBox(box, reader);
             * break;
             * case lsel:
             * box = new LayerSelectorProperty(box, reader);
             * break;
             * case imir:
             * box = new ImageMirror(box, reader);
             * break;
             * case oinf:
             * box = new OperatingPointsInformationProperty(box, reader);
             * break;
             * case udes:
             * box = new UserDescriptionBox(box, reader);
             * break;
             * case moov:
             * box = new MovieBox(box, reader);
             * break;
             */
            default:
                return box;
        }
    }

    public static String peekBoxType(SequentialByteReader reader)
    {
        reader.mark();
        reader.skip(4); // size

        String boxType = new String(reader.readBytes(4), StandardCharsets.UTF_8);
        reader.reset();

        return boxType;
    }
}