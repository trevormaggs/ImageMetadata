package heif.boxes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import common.ByteValueConverter;
import common.SequentialByteReader;

/**
 * Represents the {@code iinf} (Item Information Box), which describes items within the HEIF file.
 * This is often used to locate EXIF metadata, thumbnails, or other auxiliary images.
 * 
 * <p>
 * Specification Reference: ISO/IEC 14496-12:2015, Pages 81–83.
 * </p>
 * 
 * <p>
 * Version History:
 * </p>
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 31 May 2025</li>
 * </ul>
 * 
 * <p>
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 * 
 * @author Trevor Maggs
 * @since 31 May 2025
 */
public class ItemInformationBox extends FullBox
{
    private static final String TYPE_URI = "uri ";
    private static final String TYPE_MIME = "mime";
    private static final String TYPE_EXIF = "Exif";
    private final long entryCount;
    private final List<ItemInfoEntry> entries;

    private static int j;

    /**
     * Parses the {@code ItemInformationBox} from the specified reader.
     *
     * @param box
     *        the parent box header
     * @param reader
     *        the sequential byte reader for HEIF content
     */
    public ItemInformationBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        List<ItemInfoEntry> tmpEntries = new ArrayList<>();
        long pos = reader.getCurrentPosition();

        j = 1;

        this.entryCount = (getVersion() == 0) ? reader.readUnsignedShort() : reader.readUnsignedInteger();

        for (int i = 0; i < entryCount; i++)
        {
            tmpEntries.add(new ItemInfoEntry(new Box(reader), reader));
        }

        this.entries = Collections.unmodifiableList(tmpEntries);

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns the list of all {@link ItemInfoEntry} entries in this box.
     *
     * @return an unmodifiable list of {@code ItemInfoEntry}
     */
    public List<ItemInfoEntry> getEntries()
    {
        return entries;
    }

    /**
     * Checks whether this {@code ItemInformationBox} contains an EXIF metadata reference.
     *
     * @return boolean true if an EXIF reference exists, otherwise false
     */
    public boolean containsExif()
    {
        for (ItemInfoEntry infe : entries)
        {
            if (infe.isExif())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieves the Item ID associated with the EXIF metadata entry.
     *
     * @return the EXIF Item ID if present, otherwise -1
     */
    public int findExifItemID()
    {
        for (ItemInfoEntry infe : entries)
        {
            if (infe.getItemType() != null && infe.isExif())
            {
                return infe.getItemID();
            }
        }

        return -1;
    }

    /**
     * Retrieves the {@link ItemInfoEntry} matching the given {@code itemID}.
     *
     * @param itemID
     *        the item ID to search for
     * 
     * @return an Optional containing the matching entry if found, otherwise Optional.empty() is
     *         returned
     */
    public Optional<ItemInfoEntry> getEntry(int itemID)
    {
        for (ItemInfoEntry infe : entries)
        {
            if (infe.itemID == itemID)
            {
                return Optional.ofNullable(infe);
            }
        }

        return Optional.empty();
    }

    /**
     * Returns a combined list of all boxes contained in this {@code ItemInformationBox}, including
     * the ItemInfoEntry boxes ({@code infe}).
     * 
     * @return a combined list of Box objects in reading order
     */
    @Override
    public List<Box> getBoxList()
    {
        List<Box> combinedList = new ArrayList<>();

        combinedList.addAll(entries);

        return combinedList;
    }

    /**
     * Returns a formatted string describing the box contents.
     *
     * @return a string representation of this {@code ItemInformationBox} resource
     */
    @Override
    public String toString()
    {
        return toString(null);
    }

    /**
     * Returns a human-readable debug string, summarising structured references associated with this
     * HEIF-based file. Useful for logging or diagnostics.
     * 
     * @param prefix
     *        Optional heading or label to prepend. Can be null
     * 
     * @return a formatted string suitable for debugging, inspection, or textual analysis
     */
    @Override
    public String toString(String prefix)
    {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isEmpty())
        {
            sb.append(prefix);
        }

        for (int i = 0; i < getHierarchyDepth(); i++)
        {
            sb.append("\t");
        }

        sb.append(String.format("%s '%s':\tItem_count=%d", this.getClass().getSimpleName(), getTypeAsString(), entryCount));
        sb.append(System.lineSeparator());

        return sb.toString();
    }

    /**
     * Represents an {@code infe} (Item Info Entry) box inside an {@code iinf} box.
     */
    public static class ItemInfoEntry extends FullBox
    {
        private final int itemID;
        private final int itemProtectionIndex;
        private final String itemType;
        private final String itemName;
        private final String contentType;
        private final String contentEncoding;
        private final String itemUriType;
        private final String extensionType;
        private final boolean exifID;

        /**
         * Parses an {@code ItemInfoEntry} from the specified reader.
         *
         * @param box
         *        the parent box header
         * @param reader
         *        the byte reader for entry content
         */
        public ItemInfoEntry(Box box, SequentialByteReader reader)
        {
            super(box, reader);

            byte[] payload = reader.readBytes(available());
            String[] items;
            int version = getVersion();

            String type = null;
            String name = null;
            String cType = null;
            String encoding = null;
            String uri = null;
            String extType = null;

            if (version == 0 || version == 1)
            {
                this.itemID = ByteValueConverter.toUnsignedShort(payload, 0, box.getByteOrder());
                this.itemProtectionIndex = ByteValueConverter.toUnsignedShort(payload, 2, box.getByteOrder());

                items = ByteValueConverter.splitNullDelimitedStrings(Arrays.copyOfRange(payload, 4, payload.length));

                if (items.length > 0)
                {
                    name = items[0];
                    cType = items.length > 1 ? items[1] : null;
                    encoding = items.length > 2 ? items[2] : null;
                    extType = (version == 1 && items.length > 3) ? items[3] : null;
                }
            }

            else
            {
                int index = (version == 2) ? 2 : 4;

                this.itemID = (version == 2 ? ByteValueConverter.toUnsignedShort(payload, 0, box.getByteOrder()) : ByteValueConverter.toInteger(payload, 0, box.getByteOrder()));

                this.itemProtectionIndex = ByteValueConverter.toUnsignedShort(payload, index, box.getByteOrder());
                index += 2;

                type = new String(Arrays.copyOfRange(payload, index, index + 4), StandardCharsets.UTF_8);
                index += 4;

                items = ByteValueConverter.splitNullDelimitedStrings(Arrays.copyOfRange(payload, index, payload.length));

                if (items.length > 0)
                {
                    name = items[0];

                    if (TYPE_MIME.equals(type))
                    {
                        cType = items.length > 1 ? items[1] : null;
                        encoding = items.length > 2 ? items[2] : null;
                    }

                    else if (TYPE_URI.equals(type))
                    {
                        uri = items.length > 1 ? items[1] : null;
                    }
                }
            }

            this.itemType = type;
            this.itemName = name;
            this.contentType = cType;
            this.contentEncoding = encoding;
            this.itemUriType = uri;
            this.extensionType = extType;
            this.exifID = TYPE_EXIF.equalsIgnoreCase(type);
        }

        /**
         * Returns the Item ID.
         *
         * @return the item ID
         */
        public int getItemID()
        {
            return itemID;
        }

        /**
         * Indicates if this entry refers to EXIF data.
         *
         * @return boolean true if this is an EXIF reference, otherwise false
         */
        public boolean isExif()
        {
            return exifID;
        }

        /**
         * Returns the protection index of this item.
         *
         * @return the item protection index (0 if unprotected)
         */
        public int getItemProtectionIndex()
        {
            return itemProtectionIndex;
        }

        /**
         * Returns the item type as a 4-character code.
         *
         * @return the item type if present
         */
        public String getItemType()
        {
            return (itemType == null ? "" : itemType);
        }

        /**
         * Returns the item name.
         *
         * @return the item name if present
         */
        public String getItemName()
        {
            return (itemName == null ? "" : itemName);
        }

        /**
         * Returns the content type for MIME entries.
         *
         * @return the content type if present
         */
        public String getContentType()
        {
            return (contentType == null ? "" : contentType);
        }

        /**
         * Returns the URI type for URI entries.
         *
         * @return the URI type if present
         */
        public String getItemUriType()
        {
            return (itemUriType == null ? "" : itemUriType);
        }

        /**
         * Returns the content encoding for MIME entries.
         *
         * @return the encoding if present
         */
        public String getContentEncoding()
        {
            return (contentEncoding == null ? "" : contentEncoding);
        }

        /**
         * Returns the extension type for version 1 entries.
         *
         * @return the extension type if present
         */
        public String getExtensionType()
        {
            return (extensionType == null ? "" : extensionType);
        }

        @Override
        public String toString(String prefix)
        {
            StringBuilder sb = new StringBuilder();

            if (prefix != null && !prefix.isEmpty())
            {
                sb.append(prefix);
            }

            for (int i = 0; i < getHierarchyDepth(); i++)
            {
                sb.append("\t");
            }

            sb.append(String.format("%d)\t'%s': item_ID=%d,\titem_type='%s'", j++, getTypeAsString(), getItemID(), getItemType()));
            sb.append(System.lineSeparator());

            return sb.toString();
        }
    }
}