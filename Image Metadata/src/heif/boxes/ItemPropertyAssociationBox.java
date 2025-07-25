package heif.boxes;

import common.SequentialByteReader;

/**
 * Represents the {@code ipma} (Item Property Association Box) in HEIF/ISOBMFF files.
 *
 * <p>
 * The {@code ipma} box defines associations between items and their properties. Each item can
 * reference multiple properties, and each property can be marked as essential or non-essential for
 * decoding the item.
 * </p>
 *
 * <p>
 * This class supports both version 0 and version 1 of the {@code ipma} box format. The structure is
 * specified in the ISO/IEC 23008-12:2017 (HEIF) on Page 28 document.
 * </p>
 *
 * <h3>Version History:</h3>
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 2 June 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 2 June 2025
 * @implNote Further testing is required to confirm full compliance and interoperability.
 */
public class ItemPropertyAssociationBox extends FullBox
{
    /** The number of property association entries. */
    private final int entryCount;

    /** The array of item property entries, each describing associations for one item. */
    private final ItemPropertyEntry[] entries;

    /**
     * Constructs an {@code ItemPropertyAssociationBox} object, parsing its structure from the
     * specified {@link SequentialByteReader}.
     *
     * @param box
     *        the base {@link Box} object containing size and type information
     * @param reader
     *        the {@link SequentialByteReader} for sequential byte access
     */
    public ItemPropertyAssociationBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        entryCount = (int) reader.readUnsignedInteger();
        entries = new ItemPropertyEntry[entryCount];

        for (int i = 0; i < entryCount; i++)
        {
            int itemID = (getVersion() < 1) ? reader.readUnsignedShort() : (int) reader.readUnsignedInteger();
            int associationCount = reader.readUnsignedByte();
            ItemPropertyEntry entry = new ItemPropertyEntry(itemID, associationCount);

            for (int j = 0; j < associationCount; j++)
            {
                int value;
                boolean essential;
                int propertyIndex;

                if (getBitFlags().get(0))
                {
                    value = (int) reader.readUnsignedShort();

                    essential = ((value & 0x8000) != 0);
                    propertyIndex = (value & 0x7FFF);
                }

                else
                {
                    value = reader.readUnsignedByte();

                    essential = ((value & 0x80) != 0);
                    propertyIndex = (value & 0x7F);
                }

                entry.setAssociation(j, essential, propertyIndex);
            }

            entries[i] = entry;
        }

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * @return the number of item property association entries.
     */
    public int getEntryCount()
    {
        return entryCount;
    }

    /**
     * @return the list of {@link ItemPropertyEntry} objects.
     */
    public ItemPropertyEntry[] getEntries()
    {
        return entries;
    }

    /**
     * Returns a string representation of this {@code ItemPropertyAssociationBox}.
     *
     * @return a formatted string describing the box contents.
     */
    @Override
    public String toString()
    {
        return toString(null);
    }

    /**
     * Returns a detailed string representation of this box with an optional prefix.
     *
     * @param prefix
     *        an optional label to prepend; may be {@code null}.
     * 
     * @return a formatted string suitable for logging or inspection
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

        sb.append(String.format("%s '%s': entry_count=%d%n", this.getClass().getSimpleName(), getTypeAsString(), entryCount));

        for (int i = 0; i < entries.length; i++)
        {
            ItemPropertyEntry entry = entries[i];
            sb.append(String.format("\t\t\t%d)\titem_ID=%d, association_count=%d%n", i + 1, entry.getItemID(), entry.getAssociationCount()));

            for (ItemPropertyEntryAssociation assoc : entry.getAssociations())
            {
                sb.append(String.format("\t\t\t\tessential=%s, property_index=%d%n", assoc.isEssential(), assoc.getPropertyIndex()));
            }

            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    /**
     * Represents a single item's property associations in the {@code ipma} box.
     */
    public static class ItemPropertyEntry
    {
        private final int itemID;
        private final int associationCount;
        private final ItemPropertyEntryAssociation[] associations;

        /**
         * Constructs an {@code ItemPropertyEntry} with the specified item ID and number of
         * associations.
         *
         * @param itemID
         *        the identifier of the item
         * @param count
         *        the number of property associations for this item
         */
        public ItemPropertyEntry(int itemID, int count)
        {
            this.itemID = itemID;
            this.associationCount = count;
            this.associations = new ItemPropertyEntryAssociation[count];
        }

        /**
         * Sets the association at the specified index.
         *
         * @param index
         *        the index to set
         * @param essential
         *        {@code true} if the property is essential; otherwise, {@code false}
         * @param propertyIndex
         *        the 1-based index of the property in the {@code ipco} box
         */
        public void setAssociation(int index, boolean essential, int propertyIndex)
        {
            associations[index] = new ItemPropertyEntryAssociation(essential, propertyIndex);
        }

        /** @return the ID of the associated item */
        public int getItemID()
        {
            return itemID;
        }

        /** @return the number of associations for this item */
        public int getAssociationCount()
        {
            return associationCount;
        }

        /** @return the list of associations for this item */
        public ItemPropertyEntryAssociation[] getAssociations()
        {
            return associations;
        }
    }

    /**
     * Represents a single association between an item and a property.
     */
    public static class ItemPropertyEntryAssociation
    {
        private final boolean essential;
        private final int propertyIndex;

        /**
         * Constructs an association between an item and a property.
         *
         * @param essential
         *        whether the property is essential
         * @param propertyIndex
         *        the 1-based index of the property in the {@code ipco} box
         */
        public ItemPropertyEntryAssociation(boolean essential, int propertyIndex)
        {
            this.essential = essential;
            this.propertyIndex = propertyIndex;
        }

        /**
         * Returns the Essential value as a boolean value.
         * 
         * @return {@code true} if the property is essential, otherwise {@code false}
         */
        public boolean isEssential()
        {
            return essential;
        }

        /**
         * Returns the Property Index.
         * 
         * @return the 1-based property index in the {@code ipco} box
         */
        public int getPropertyIndex()
        {
            return propertyIndex;
        }
    }
}