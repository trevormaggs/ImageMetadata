package heif.boxes;

import common.SequentialByteReader;
import logger.LogFactory;

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
 * <p>
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class ItemPropertyAssociationBox extends FullBox
{
    private static final LogFactory LOGGER = LogFactory.getLogger(ItemPropertyAssociationBox.class);
    private final int entryCount;
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

        long pos = reader.getCurrentPosition();

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
                    value = reader.readUnsignedShort();

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
     * Logs a single diagnostic line for this box at the debug level.
     *
     * <p>
     * This is useful when traversing the box tree of a HEIF/ISO-BMFF structure for debugging or
     * inspection purposes.
     * </p>
     */
    @Override
    public void logBoxInfo()
    {
        String tab = Box.repeatPrint("\t", getHierarchyDepth());
        LOGGER.debug(String.format("%s%s '%s': entry_count=%d", tab, this.getClass().getSimpleName(), getTypeAsString(), entryCount));

        for (int i = 0; i < entries.length; i++)
        {
            ItemPropertyEntry entry = entries[i];

            LOGGER.debug(String.format("\t\t\t%d)\titem_ID=%d, association_count=%d", i + 1, entry.getItemID(), entry.getAssociationCount()));

            for (ItemPropertyEntryAssociation assoc : entry.getAssociations())
            {
                LOGGER.debug(String.format("\t\t\t\tessential=%s, property_index=%d", assoc.isEssential(), assoc.getPropertyIndex()));
            }
        }
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