package heif.boxes;

import java.util.ArrayList;
import java.util.List;
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
    private final DataReferenceBox dref;

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
     * Returns a combined list of all boxes contained in this {@code DataInformationBox},
     * particularly the DataReferenceBox ({@code dref}).
     *
     * @return a combined list of Box objects in reading order
     */
    @Override
    public List<Box> getBoxList()
    {
        List<Box> combinedList = new ArrayList<>();
        combinedList.add(dref);

        return combinedList;
    }

    /**
     * Returns a string representation of this {@code DataInformationBox}.
     *
     * @return a formatted string describing the box contents.
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

        sb.append(String.format("%s '%s':\t(%s)", this.getClass().getSimpleName(), getTypeAsString(), getHeifType().getBoxCategory()));
        sb.append(System.lineSeparator());

        return sb.toString();
    }

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

            entryCount = (int) reader.readUnsignedInteger();
            dataEntry = new DataEntryBox[entryCount];

            for (int i = 0; i < entryCount; i++)
            {
                dataEntry[i] = new DataEntryBox(new Box(reader), reader);
            }
        }

        /**
         * Returns a string representation of this {@code DataReferenceBox}.
         *
         * @return a formatted string describing the box contents.
         */
        @Override
        public String toString()
        {
            return toString(null);
        }

        /**
         * Returns a human-readable debug string, summarising structured references associated with
         * this HEIF-based file. Useful for logging or diagnostics.
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

            sb.append(String.format("\t%s '%s':\tentryCount=%d%n", this.getClass().getSimpleName(), getTypeAsString(), entryCount));

            for (int i = 0; i < entryCount; i++)
            {
                sb.append(String.format("\t\t\tName: '%s'\tLocation: '%s'%n", dataEntry[i].name, dataEntry[i].location));
            }

            return sb.toString();
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

            if (available() > 0)
            {
                String[] parts = ByteValueConverter.splitNullDelimitedStrings(reader.readBytes((int) getBoxSize()));

                if (getBitFlags().get(0) && parts.length > 0)
                {
                    if (getTypeAsString().contains("url"))
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
        }
    }
}