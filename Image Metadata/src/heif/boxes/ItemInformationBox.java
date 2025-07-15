package heif.boxes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import common.ByteValueConverter;
import common.SequentialByteReader;

/**
 * This derived Box class handles the Box identified as {@code iinf} - Item Information Box. For
 * technical details, refer to the Specification document - {@code ISO/IEC 14496-12:2015} on Page 81
 * to 83.
 * 
 * <p>
 * Version History:
 * </p>
 *
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 31 May 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 31 May 2025
 * @implNote Additional testing is required to confirm the reliability and robustness of this
 *           implementation
 */
public class ItemInformationBox extends FullBox
{
    private long entryCount;
    private List<Box> infeList;

    /**
     * This constructor creates a derived Box object, providing additional information about
     * selected items. This aids in determining if HEIC image files contain an embedded EXIF
     * segment.
     *
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public ItemInformationBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        infeList = new ArrayList<>();
        entryCount = (getVersion() == 0 ? reader.readUnsignedShort() : reader.readUnsignedInteger());

        for (int i = 0; i < entryCount; i++)
        {
            infeList.add(new ItemInfoEntry(new Box(reader), reader));
        }

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Checks whether this entry is a reference to an Exif block that may be present in the HEIF box structure.
     *
     * @return true if this entry is an Exif reference
     */
    public boolean hasExifBlock()
    {
        for (Box box : infeList)
        {
            ItemInfoEntry infe = ((ItemInfoEntry) box);

            if (infe.isExif())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the Item ID used for identifying the Exif block.
     *
     * @return a positive Exif ID number, otherwise a value of -1 is returned if no Exif information
     *         is present
     */
    public int getExifID()
    {
        for (Box box : infeList)
        {
            ItemInfoEntry infe = ((ItemInfoEntry) box);

            if (infe.isExif())
            {
                return infe.getItemID();
            }
        }

        return -1;
    }

    /**
     * Searches and finds and returns the Item Information Entry box entry based on the specified
     * item ID number.
     *
     * @param itemID
     *        an ID number identifying the box
     *
     * @return an ItemInfoEntry object if a match is found, otherwise null
     */
    public ItemInfoEntry getEntry(int itemID)
    {
        for (Box box : infeList)
        {
            ItemInfoEntry infe = ((ItemInfoEntry) box);

            if (infe.itemID == itemID)
            {
                return infe;
            }
        }

        return null;
    }

    /**
     * Displays a list of structured references associated with the specified HEIF based file,
     * useful for analytical purposes.
     *
     * @return the string
     */
    @Override
    public String showBoxStructure()
    {
        int j = 1;
        StringBuilder line = new StringBuilder();

        line.append(String.format("\t%s '%s':\tItem_count=%d%n", this.getClass().getSimpleName(), getBoxName(), entryCount));

        for (Box box : infeList)
        {
            ItemInfoEntry infe = ((ItemInfoEntry) box);

            line.append(String.format("\t\t%d)\t'%s': item_ID=%d,\titem_type='%s'%n", j++, infe.getBoxName(), infe.itemID, infe.itemType));
        }

        return line.toString();
    }

    /**
     * Generates a string representation of the derived Box structure.
     *
     * @return a formatted string
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(super.toString());

        for (Box box : infeList)
        {
            ItemInfoEntry infe = ((ItemInfoEntry) box);

            line.append(System.lineSeparator());
            line.append(String.format("  \t%-24s %s%n", "[Version]", infe.getVersion()));
            line.append(String.format("  \t%-24s %s%n", "[Item ID]", infe.getItemID()));
            line.append(String.format("  \t%-24s %s%n", "[Exif Block]", infe.isExif()));
            line.append(String.format("  \t%-24s %s%n", "[Item Protection Index]", infe.getItemProtectionIndex()));
            line.append(String.format("  \t%-24s %s%n", "[Item Type]", infe.getItemType()));
            line.append(String.format("  \t%-24s %s%n", "[Item Name]", infe.getItemName()));
            line.append(String.format("  \t%-24s %s%n", "[Content Type]", infe.getContentType()));
            line.append(String.format("  \t%-24s %s%n", "[Content Encoding]", infe.getContentEncoding()));
            line.append(String.format("  \t%-24s %s%n", "[Item URI Type]", infe.getItemUriType()));
            line.append(String.format("  \t%-24s %s%n", "[Extension Type]", infe.getExtensionType()));
        }

        return line.toString();
    }

    /**
     * A nested class used to manage the {@code ItemInfoEntry} box. This box type is known as
     * {@code infe} as part of the Item Information Box.
     */
    private static class ItemInfoEntry extends FullBox
    {
        private int itemID;
        private int itemProtectionIndex;
        private String itemType;
        private String itemName;
        private String contentType;
        private String contentEncoding;
        private String itemUriType;
        private String extensionType;
        private boolean exifID;

        /**
         * This constructor creates a derived Box object, providing additional information about
         * selected items. This aids in determining if HEIC image files contain an embedded EXIF
         * directory.
         *
         * @param box
         *        the super Box object
         * @param reader
         *        a SequentialByteReader object for sequential byte array access
         */
        private ItemInfoEntry(Box box, SequentialByteReader reader)
        {
            super(box, reader);

            String[] items;
            int version = getVersion();
            byte[] payload = reader.readBytes(available());

            if (version == 0 || version == 1)
            {
                itemID = ByteValueConverter.toUnsignedShort(payload, 0, box.getByteOrder());
                itemProtectionIndex = ByteValueConverter.toUnsignedShort(payload, 2, box.getByteOrder());

                items = ByteValueConverter.splitNullDelimitedStrings(Arrays.copyOfRange(payload, 4, payload.length));

                if (items.length > 0)
                {
                    itemName = items[0];
                    contentType = (items.length > 1 ? items[1] : "");
                    contentEncoding = (items.length > 2 ? items[2] : "");
                    extensionType = (version == 1 && items.length > 3 ? items[3] : "");
                    // this is optional ItemInfoExtension(extensionType);
                }
            }

            if (version > 1)
            {
                int index = 2;

                if (version == 2)
                {
                    // Length: 2 bytes
                    itemID = ByteValueConverter.toUnsignedShort(payload, 0, box.getByteOrder());
                }

                else if (version == 3)
                {
                    // Length: 4 bytes
                    itemID = ByteValueConverter.toInteger(payload, 0, box.getByteOrder());
                    index = 4;
                }

                // Length: 2 bytes
                itemProtectionIndex = ByteValueConverter.toUnsignedShort(payload, index, box.getByteOrder());
                index += 2;

                // Length: 4 bytes
                itemType = new String(Arrays.copyOfRange(payload, index, index + 4), StandardCharsets.UTF_8);
                index += 4;

                // Length: variable
                items = ByteValueConverter.splitNullDelimitedStrings(Arrays.copyOfRange(payload, index, payload.length));

                if (items.length > 0)
                {
                    itemName = items[0];

                    if (itemType.equals("mime"))
                    {
                        contentType = (items.length > 1 ? items[1] : "");
                        contentEncoding = (items.length > 2 ? items[2] : "");
                    }

                    else if (itemType.equals("uri "))
                    {
                        itemUriType = (items.length > 1 ? items[1] : "");
                    }
                }
            }

            if (itemType.equalsIgnoreCase("Exif"))
            {
                exifID = true;
            }
        }

        /**
         * Returns the ID of the item.
         * 
         * @return an integer representing the Item ID
         */
        private int getItemID()
        {
            return itemID;
        }

        /**
         * Returns true if this entry references to an EXIF segment block.
         * 
         * @return true if the Exif structure exists, otherwise false
         */
        private boolean isExif()
        {
            return exifID;
        }

        /**
         * Returns the a value either 0 for an unprotected item, or the one-based index into the
         * item protection box defining the protection applied to this item (the first box in the
         * item protection box has the index 1).
         * 
         * @return a long with the Item Protection Index data
         */
        private long getItemProtectionIndex()
        {
            return itemProtectionIndex;
        }

        /**
         * Returns the 32-bit value, typically 4 printable characters, that is a defined valid item
         * type indicator, such as {@code mime}.
         * 
         * @return string
         */
        private String getItemType()
        {
            return (itemType == null ? "" : itemType);
        }

        /**
         * Returns the symbolic name of the item.
         * 
         * @return string
         */
        private String getItemName()
        {
            return (itemName == null ? "" : itemName);
        }

        /**
         * Returns the content type of the item.
         * 
         * @return string
         */
        private String getContentType()
        {
            return (contentType == null ? "" : contentType);
        }

        /**
         * Returns the absolute URI, that is used as a type indicator.
         * 
         * @return string
         */
        private String getItemUriType()
        {
            return (itemUriType == null ? "" : itemUriType);
        }

        /**
         * Returns the string used to indicate that the binary file is encoded and needs to be
         * decoded before interpreted.
         * 
         * @return string
         */
        private String getContentEncoding()
        {
            return (contentEncoding == null ? "" : contentEncoding);
        }

        /**
         * Returns the printable four-character code that identifies the extension fields of version
         * 1 with respect to version 0 of the Item information entry.
         * 
         * @return string
         */
        private String getExtensionType()
        {
            return (extensionType == null ? "" : extensionType);
        }
    }
}