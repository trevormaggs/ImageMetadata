package heif.boxes;

import java.util.BitSet;
import heif.HeifBoxType;
import common.SequentialByteReader;

/**
 * This code creates a complete class that stores ubiquitous high-level data applicable for the
 * majority of the derived Box objects, serving as the complete primary header box. It maintains
 * details such as the entire size, including the size and type header, fields, and potentially
 * contained boxes. This feature supports the parsing process of the HEIC file.
 *
 * For further technical details, refer to the Specification document - ISO/IEC 14496-12:2015 on
 * Pages 6 and 7 under {@code Object Structure}.
 *
 * <p>
 * Version History:
 * </p>
 *
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 28 May 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 28th May 2025
 */
public class FullBox extends Box
{
    private int version;
    private BitSet flagBit;

    /**
     * This constructor creates a derived Box object, extending the parent class {@code Box} to
     * provide additional information.
     *
     * @param box
     *        the parent Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public FullBox(Box box, SequentialByteReader reader)
    {
        super(box);

        int pos = reader.getCurrentPosition();

        /* Total 4 bytes are read here plus 8 bytes in the parent Box class */
        version = reader.readUnsignedByte();

        byte[] b = reader.readBytes(3);

        /*
         * Because the static valueOf method requires the byte array to be a little-endian
         * representation. Therefore, we need to reverse the bytes in the array first since the HEIF
         * based files follows the big-endian format.
         * 
         * See: https://docs.oracle.com/javase/8/docs/api/java/util/BitSet.html#valueOf-byte:A-
         */
        byte[] reversed = new byte[b.length];

        for (int i = 0; i < b.length; i++)
        {
            reversed[i] = b[b.length - 1 - i];
        }

        flagBit = BitSet.valueOf(reversed);

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * This copy constructor creates a new FullBox object by copying all field variables from
     * another FullBox object.
     *
     * @param box
     *        the other FullBox object to copy from
     */
    public FullBox(FullBox box)
    {
        super(box);

        this.version = box.version;
        this.flagBit = box.flagBit;
    }

    /**
     * Returns the integer identifying the version of this particular box's format details.
     *
     * @return A byte value representing the version
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Returns an array of bytes containing flag information.
     *
     * @return an array of bytes
     */
    public byte[] getBoxFlags()
    {
        return flagBit.toByteArray();
    }

    /**
     * Returns a bitmap containing flag information.
     *
     * @return a BitSet object
     */
    public BitSet BitFlags()
    {
        return flagBit;
    }

    /**
     * Returns a string that represents the binary form of the flag map.
     *
     * @return string exhibiting the binary representation
     */
    public String formatFlagString()
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < flagBit.length(); i++)
        {
            sb.append(flagBit.get(i) == true ? 1 : 0);
        }

        return sb.toString();
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

        line.append(String.format("%s '%s':\t\t\t\t\t(%s)", this.getClass().getSimpleName(), getBoxName(), box.getBoxCategory()));        

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
        line.append(String.format("  %-24s %s%n", "[Version]", getVersion()));
        line.append(String.format("  %-24s %s%n", "[Flags]", formatFlagString()));

        return line.toString();
    }
}