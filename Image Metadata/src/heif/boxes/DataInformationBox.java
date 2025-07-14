package heif.boxes;

import heif.HeifBoxType;
import common.ByteValueConverter;
import common.SequentialByteReader;

/**
 * This derived Box class handles the Box identified as {@code dinf} - Data Information Box. For
 * technical details, refer to the Specification document - {@code ISO/IEC 14496-12:2015} on Page 45
 * to 46.
 * 
 * The data information box contains objects that declare the location of the media information in a
 * track.
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
 * @implNote Additional testing is required to validate the reliability and robustness of this
 *           implementation
 */  
public class DataInformationBox extends Box
{
    private DataReferenceBox dref;

    /**
     * An inner class designed to fill up the {@code dref} box type.
     */
    public static class DataReferenceBox extends FullBox
    {
        public int entryCount;
        public DataEntryBox[] dataEntry;

        public DataReferenceBox(Box box, SequentialByteReader reader)
        {
            super(box, reader);

            int pos = reader.getCurrentPosition();

            entryCount = (int) reader.readUnsignedInteger();
            dataEntry = new DataEntryBox[entryCount];

            byteUsed += reader.getCurrentPosition() - pos;

            for (int i = 0; i < entryCount; i++)
            {
                dataEntry[i] = new DataEntryBox(new Box(reader), reader);
            }
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

            line.append(System.lineSeparator());
            line.append(String.format("\t\t%s '%s':\tentryCount=%d", this.getClass().getSimpleName(), getBoxName(), entryCount));
            line.append(System.lineSeparator());
            
            for (int i = 0; i < entryCount; i++)
            {
                line.append(String.format("\t\t\tName: '%s'\tLocation: '%s'", dataEntry[i].name, dataEntry[i].location));
            }

            return line.toString();
        }

        /**
         * Generates a string representation of the basic Box structure.
         *
         * @return a formatted string
         */
        @Override
        public String toString()
        {
            StringBuilder line = new StringBuilder();

            line.append(super.toString());
            line.append(System.lineSeparator());

            for (int i = 0; i < entryCount; i++)
            {
                DataEntryBox entry = dataEntry[i];

                line.append(entry.toString());
                line.append(System.lineSeparator());
                line.append(String.format("  \t%-20s %s%n", "[Name]", entry.name.isEmpty() ? "<Empty>" : entry.name));
                line.append(String.format("  \t%-20s %s%n", "[Location]", entry.location.isEmpty() ? "<Empty>" : entry.location));
            }

            return line.toString();
        }
    }

    /**
     * An inner class used to store a {@code DataEntryBox} object, containing information such as
     * URL location and name.
     */
    public static class DataEntryBox extends FullBox
    {
        public String name = "";
        public String location = "";

        public DataEntryBox(Box box, SequentialByteReader reader)
        {
            super(box, reader);

            int pos = reader.getCurrentPosition();

            if (remainingBytes() > 0)
            {
                String[] parts = ByteValueConverter.splitNullDelimitedStrings(reader.readBytes(getBoxSize()));

                if (BitFlags().get(0) && parts.length > 0)
                {
                    if (getBoxName().contains("url"))
                    {
                        location = parts[0];
                    }

                    else // urn
                    {
                        name = parts[0];
                        location = (parts.length > 1 ? parts[1] : "");
                    }
                }
            }

            byteUsed += reader.getCurrentPosition() - pos;
        }
    }

    /**
     * This constructor creates a derived Box object, providing additional information from other
     * contained boxes, specifically {@code dref} - Data Reference Box and its nested contained
     * boxes, where further additional information on URL location and name is provided.
     * 
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public DataInformationBox(Box box, SequentialByteReader reader)
    {
        super(box);

        int pos = reader.getCurrentPosition();
        dref = new DataReferenceBox(new Box(reader), reader);
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
        HeifBoxType box = HeifBoxType.getBoxType(getBoxName());

        line.append(String.format("\t%s '%s':\t(%s)", this.getClass().getSimpleName(), getBoxName(), box.getBoxCategory()));
        line.append(dref.showBoxStructure());

        return line.toString();
    }

    /**
     * Generates a string representation of the basic Box structure.
     *
     * @return a formatted string
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(super.toString());
        line.append(System.lineSeparator());
        line.append(dref);

        return line.toString();
    }
}