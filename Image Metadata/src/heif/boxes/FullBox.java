package heif.boxes;

import java.util.BitSet;
import common.SequentialByteReader;
import logger.LogFactory;

/**
 * This code creates a complete class that stores ubiquitous high-level data applicable for the
 * majority of the derived Box objects, serving as the complete primary header box. It maintains
 * details such as the entire size, including the size and type header, fields, and potentially
 * contained boxes. This feature supports the parsing process of the HEIC file.
 *
 * For further technical details, refer to the Specification document - ISO/IEC 14496-12:2015 on
 * Pages 6 and 7 under {@code Object Structure}.
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class FullBox extends Box
{
    private static final LogFactory LOGGER = LogFactory.getLogger(FullBox.class);
    private final int version;
    private final BitSet flagBit;

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

        long pos = reader.getCurrentPosition();

        /* Reads 4 additional bytes (1 byte version + 3 bytes flags), on top of the Box header */
        version = reader.readUnsignedByte();

        byte[] b = reader.readBytes(3);

        /*
         * The static BitSet.valueOf(byte[]) method expects the input array to be in little-endian
         * order. Since HEIF files use big-endian format, we need to reverse the byte array before
         * calling valueOf.
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
        this.flagBit = (BitSet) box.flagBit.clone();
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
    public BitSet getBitFlags()
    {
        return (BitSet) flagBit.clone();
    }

    public int getFlagsAsInt()
    {
        int result = 0;
        byte[] bytes = flagBit.toByteArray();

        for (int i = 0; i < bytes.length; i++)
        {
            result |= (bytes[i] & 0xFF) << (8 * i);
        }

        return result;
    }
    /**
     * Returns a string that represents the binary form of the flag map.
     *
     * @return string exhibiting the binary representation
     */
    public String getFlagsAsBinaryString()
    {
        StringBuilder sb = new StringBuilder();

        for (int i = flagBit.length() - 1; i >= 0; i--)
        {
            sb.append(flagBit.get(i) ? '1' : '0');
        }

        return sb.toString();
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
        LOGGER.debug(String.format("%s%s '%s':%s(%s)", tab, this.getClass().getSimpleName(), getTypeAsString(), tab, getHeifType().getBoxCategory()));
    }
}