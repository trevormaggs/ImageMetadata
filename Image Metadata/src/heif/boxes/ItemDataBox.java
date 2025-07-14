package heif.boxes;

import java.util.Arrays;
import common.SequentialByteReader;

/**
 * This derived Box class handles the Box identified as {@code idat} - Item Data Box. For technical
 * details, refer to the Specification document - {@code ISO/IEC 23008-12:2017} on Page 86.
 *
 * This box contains the data of metadata items that use the construction method indicating that an
 * item’s data extents are stored within this box.
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
public class ItemDataBox extends Box
{
    private int[] data;

    /**
     * This constructor creates a derived Box object, providing additional data of metadata items.
     * 
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public ItemDataBox(Box box, SequentialByteReader reader)
    {
        super(box);

        int count = remainingBytes();
        int pos = reader.getCurrentPosition();

        data = new int[count];

        for (int i = 0; i < count; i++)
        {
            data[i] = reader.readUnsignedByte();
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

        line.append(String.format("\t%s '%s':", this.getClass().getSimpleName(), getBoxName()));
        line.append(System.lineSeparator());
        line.append(String.format("\t\tData bytes: "));

        for (int i = 0; i < data.length; i++)
        {
            line.append(String.format("0x%02X ", data[i]));
            // line.append(String.format("%d ", data[i]));
        }
        
        line.append(System.lineSeparator());

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
        line.append(String.format("  %-24s %s%n", "[Data]", Arrays.toString(data)));

        return line.toString();
    }
}