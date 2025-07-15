package heif.boxes;

import java.util.ArrayList;
import java.util.List;
import common.SequentialByteReader;

/**
 * This derived class handles the Box identified as {@code iref} - Item Reference Box. For technical
 * details, refer to the Specification document - {@code ISO/IEC 14496-12:2015} on Page 87.
 * 
 * Basically, it allows the linking of one item to others via typed references. All references for
 * one item of a specific type are collected into a single item type reference box.
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
public class ItemReferenceBox extends FullBox
{
    private List<Box> references;

    /**
     * An inner class designed to fill up a {@code SingleItemTypeReferenceBox} box for referencing
     * purposes.
     */
    public static class SingleItemTypeReferenceBox extends Box
    {
        private long fromItemID;
        private int referenceCount;
        private long[] toItemID;

        public SingleItemTypeReferenceBox(Box box, SequentialByteReader reader, boolean large)
        {
            super(box);

            fromItemID = (large ? reader.readUnsignedInteger() : reader.readUnsignedShort());
            referenceCount = reader.readUnsignedShort();
            toItemID = new long[referenceCount];

            for (int j = 0; j < referenceCount; j++)
            {
                toItemID[j] = (large ? reader.readUnsignedInteger() : reader.readUnsignedShort());
            }
        }
    }

    /**
     * This constructor creates a derived Box object, providing additional information about the
     * linking of one item to others via typed references.
     *
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public ItemReferenceBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        references = new ArrayList<>();

        do
        {
            Box newBox = new SingleItemTypeReferenceBox(new Box(reader), reader, (getVersion() != 0));
            references.add(newBox);

        } while (reader.getCurrentPosition() < pos + available());

        byteUsed += reader.getCurrentPosition() - pos;
    }

    @Override
    public List<Box> addBoxList()
    {
        // return references;
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

        line.append(String.format("\t%s '%s':", this.getClass().getSimpleName(), getBoxName()));
        line.append(System.lineSeparator());

        for (Box list : references)
        {
            SingleItemTypeReferenceBox box = ((SingleItemTypeReferenceBox) list);
            line.append(String.format("\t\treferenceType='%s': from_item_ID=%d,\tref_count=%d,\tto_item_ID=", box.getBoxName(), box.fromItemID, box.referenceCount));

            for (int j = 0; j < box.referenceCount; j++)
            {
                if (j < box.referenceCount - 1)
                {
                    line.append(String.format("%d, ", box.toItemID[j]));
                }

                else
                {
                    line.append(String.format("%d%n", box.toItemID[j]));
                }
            }
        }

        line.append(System.lineSeparator());

        for (Box box : references)
        {
            line.append(String.format("%s%n", box.showBoxStructure()));
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

        for (Box box : references)
        {
            line.append(System.lineSeparator());

            SingleItemTypeReferenceBox ref = ((SingleItemTypeReferenceBox) box);

            line.append(String.format("  \t%-24s %s%n", "[From Item ID]", ref.fromItemID));
            line.append(String.format("  \t%-24s %s%n", "[Reference Count]", ref.referenceCount));
            line.append(String.format("  \t%-24s ", "[To Item ID]"));

            for (long item : ref.toItemID)
            {
                // line.append(String.format("0x%02X ", item));
                line.append(String.format("%d ", item));
            }

            line.append(System.lineSeparator());
        }

        return line.toString();
    }
}