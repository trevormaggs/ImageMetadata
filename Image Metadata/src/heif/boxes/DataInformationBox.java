package heif.boxes;

import java.util.ArrayList;
import java.util.List;
import common.ByteValueConverter;
import common.SequentialByteReader;
import logger.LogFactory;

/**
 * This derived Box class handles the Box identified as {@code dinf} - Data Information Box. For
 * technical details, refer to the Specification document - {@code ISO/IEC 14496-12:2015} on Page 45
 * to 46.
 *
 * The data information box contains objects that declare the location of the media information in a
 * track.
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
public class DataInformationBox extends Box
{
    private static final LogFactory LOGGER = LogFactory.getLogger(DataInformationBox.class);
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

        long pos = reader.getCurrentPosition();
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
        LOGGER.debug(String.format("%s%s '%s':\t(%s)", tab, this.getClass().getSimpleName(), getTypeAsString(), getHeifType().getBoxCategory()));
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
            LOGGER.debug(String.format("%s%s '%s':\tentryCount=%d", tab, this.getClass().getSimpleName(), getTypeAsString(), entryCount));

            for (int i = 0; i < entryCount; i++)
            {
                tab = Box.repeatPrint("\t", getHierarchyDepth() + 1);
                LOGGER.debug(String.format("%sName: '%s'\tLocation: '%s'", tab, dataEntry[i].name, dataEntry[i].location));
            }
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