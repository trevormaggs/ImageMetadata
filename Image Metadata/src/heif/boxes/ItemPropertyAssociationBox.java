package heif.boxes;

import common.SequentialByteReader;

/**
 * This derived class handles the Box identified as {@code ipma} - Item Properties Box. For
 * technical details, refer to the Specification document - {@code ISO/IEC 23008-12:2017} on Page
 * 28.
 * 
 * Basically, this {@code ItemPropertiesBox} class enables the association of any item with an
 * ordered set of item properties. Item properties are small data records.
 * 
 * <p>
 * Version History:
 * </p>
 *
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 2 June 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 2 June 2025
 * @implNote This is not a perfect solution yet. Rigorous testing is required to validate the
 *           reliability and robustness of this implementation
 */
public class ItemPropertyAssociationBox extends FullBox
{
    public int entryCount;
    public ItemPropertyEntry[] entries;

    /**
     * 
     * Refer to the {@code ISO/IEC 23008-12:2017} on Page 29 under Semantics for more information.
     * 
     * Item property entry data.
     */
    public static class ItemPropertyEntry
    {
        public int itemID;
        public int associationCount;
        public ItemPropertyEntryAssociation[] associations;

        /**
         * Refer to the {@code ISO/IEC 23008-12:2017} on Page 29 under Semantics for more
         * information.
         */
        private static class ItemPropertyEntryAssociation
        {
            /**
             * This variable is set to 1 to indicate that the associated property is essential to
             * the item, otherwise it is non-essential.
             */
            private boolean essential;

            /**
             * The variable is either 0 indicating that no property is associated (the essential
             * indicator shall also be 0), or is the 1-based index of the associated property box in
             * the ItemPropertyContainerBox contained in the same ItemPropertiesBox.
             */
            private int propertyIndex;
        }

        private ItemPropertyEntry(int itemid, int count)
        {
            this.itemID = itemid;
            this.associationCount = count;
            this.associations = new ItemPropertyEntryAssociation[count];
        }

        private void setAssociationValues(int index, boolean essential, int propertyIndex)
        {
            associations[index] = new ItemPropertyEntryAssociation();
            associations[index].essential = essential;
            associations[index].propertyIndex = propertyIndex;
        }
    }

    public ItemPropertyAssociationBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        entryCount = (int) reader.readUnsignedInteger();
        entries = new ItemPropertyEntry[entryCount];

        for (int i = 0; i < entryCount; i++)
        {
            int itemID = (int) (getVersion() < 1 ? reader.readUnsignedShort() : reader.readUnsignedInteger());
            int associationCount = reader.readUnsignedByte();

            entries[i] = new ItemPropertyEntry(itemID, associationCount);

            for (int j = 0; j < associationCount; j++)
            {
                boolean essential;
                int propertyIndex;

                if (BitFlags().get(0))
                {
                    int value = (int) (reader.readUnsignedInteger());

                    essential = (((value & 0x8000) >> 15) == 1);
                    propertyIndex = (value & 0x7FFF);
                }

                else
                {
                    int value = reader.readUnsignedByte();

                    essential = (((value & 0x80) >> 7) == 1);
                    propertyIndex = (value & 0x7F);
                }

                entries[i].setAssociationValues(j, essential, propertyIndex);
            }
        }

        byteUsed += reader.getCurrentPosition() - pos;
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
        StringBuilder line = new StringBuilder();

        line.append(String.format("\t%s '%s': entry_count=%d%n", this.getClass().getSimpleName(), getBoxName(), entryCount));
        
        for (int i = 0; i < entries.length; i++)
        {
            ItemPropertyEntry property = entries[i];

            line.append(String.format("\t\t%d)\titem_ID=%d, association_count=%d", i + 1, property.itemID, property.associationCount));
            line.append(System.lineSeparator());
            
            for (int j = 0; j < property.associationCount; j++)
            {
                ItemPropertyEntry.ItemPropertyEntryAssociation ref = property.associations[j];
                line.append(String.format("\t\t\t\tessential=%s, property_index=%d%n", ref.essential, ref.propertyIndex));
            }
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
        line.append(String.format("  %-24s %s%n", "[Entry Count]", entryCount));

        for (ItemPropertyEntry property : entries)
        {
            line.append(System.lineSeparator());
            line.append(String.format("  \t%-24s %s%n", "[Item ID]", property.itemID));
            line.append(String.format("  \t%-24s %s%n", "[Association Count]", property.associationCount));

            for (int i = 0; i < property.associationCount; i++)
            {
                ItemPropertyEntry.ItemPropertyEntryAssociation ref = property.associations[i];

                line.append(System.lineSeparator());
                line.append(String.format("  \t\t%-20s %s%n", "[Essential]", ref.essential));
                line.append(String.format("  \t\t%-20s %s%n", "[Property Index]", ref.propertyIndex));
            }
        }

        return line.toString();
    }
}