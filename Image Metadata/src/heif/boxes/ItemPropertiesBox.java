package heif.boxes;

import java.util.ArrayList;
import java.util.List;
import heif.BoxFactory;
import heif.HeifBoxType;
import common.SequentialByteReader;

/**
 * This derived class handles the Box identified as {@code iprp} - Item Properties Box. For
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
 * @implNote Additional testing is required to validate the reliability and robustness of this
 *           implementation
 */
public class ItemPropertiesBox extends Box
{
    private List<Box> associations;
    private ItemPropertyContainerBox ipco;

    /**
     * An inner class to support the nested {@code ipco} container box - ItemProperty Container Box.
     * Refer to the Specification document - {@code ISO/IEC 23008-12:2017} on Page 28 for more
     * information.
     */
    private static class ItemPropertyContainerBox extends Box
    {
        private List<Box> properties;

        /**
         * The {@code ItemPropertyContainerBox} box contains an implicitly indexed list of item
         * properties.
         * 
         * @param box
         *        the super Box object
         * @param reader
         *        a SequentialByteReader object for sequential byte array access
         */
        private ItemPropertyContainerBox(Box box, SequentialByteReader reader)
        {
            super(box);

            int pos = reader.getCurrentPosition();

            properties = new ArrayList<>();

            do
            {
                Box newBox = BoxFactory.createBox(new Box(reader), reader);

                /*
                 * Need to skip bytes in case some boxes do not have a proper
                 * handler to process them. hvcC box is one of them.
                 */
                reader.skip(newBox.remainingBytes());
                properties.add(newBox);

            } while (reader.getCurrentPosition() < pos + remainingBytes());

            byteUsed += reader.getCurrentPosition() - pos;
        }
    }

    /**
     * This constructor creates a derived Box object, providing some important information. In this
     * class, the {@code ItemPropertiesBox} box enables the association of any item with an ordered
     * set of item properties. Item properties are small data records.
     * 
     * The ItemPropertiesBox consists of two parts: {@code ItemPropertyContainerBox} (short for
     * ipco) that contains an implicitly indexed list of item properties, and one or more
     * ItemPropertyAssociation boxes that associate items with item properties.
     * 
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public ItemPropertiesBox(Box box, SequentialByteReader reader)
    {
        super(box);

        int pos = reader.getCurrentPosition();

        associations = new ArrayList<>();

        /* First part - manage ItemPropertyContainerBox */
        ipco = new ItemPropertyContainerBox(new Box(reader), reader);

        do
        {
            /* Second part - manage ItemPropertyAssociationBox */
            ItemPropertyAssociationBox impa = new ItemPropertyAssociationBox(new Box(reader), reader);

            associations.add(impa);
            // reader.skip(impa.remainingBytes());

        } while (reader.getCurrentPosition() < pos + remainingBytes());

        byteUsed += reader.getCurrentPosition() - pos;
    }

    @Override
    public List<Box> addBoxList()
    {
        List<Box> combinedList = new ArrayList<>(ipco.properties);
        combinedList.addAll(associations);

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
        StringBuilder line = new StringBuilder();

        line.append(String.format("\t%s '%s':%n", this.getClass().getSimpleName(), getBoxName()));
        line.append(String.format("\t\t%s '%s':%n", ipco.getClass().getSimpleName(), ipco.getBoxName()));

        for (Box box : ipco.properties)
        {
            line.append(String.format("\t\t\t'%s':%n", box.getBoxName()));
        }

        line.append(System.lineSeparator());

        for (Box box : ipco.properties)
        {
            if (HeifBoxType.getBoxType(box.getBoxName()) != HeifBoxType.UNKNOWN)
            {
                line.append(String.format("\t%s%n", box.showBoxStructure()));
            }

            else
            {
                line.append(String.format("\t\t\t%s%n", box.showBoxStructure()));
            }
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

        for (Box box : ipco.properties)
        {
            line.append(box);
            line.append(System.lineSeparator());
        }

        for (Box box : associations)
        {
            line.append(box);
            line.append(System.lineSeparator());
        }

        return line.toString();
    }
}