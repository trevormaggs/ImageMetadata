package heif.boxes;

import common.SequentialByteReader;

/**
 * This derived box, namely the {@code pitm} handler type, the primary data may be one of the
 * referenced items when it is desired that it be stored elsewhere, or divided into extents, or the
 * primary metadata may be contained in the meta-box (e.g. in an XML box). Either this box must
 * occur, or there must be a box within the meta-box (e.g. an XML box) containing the primary
 * information in the format required by the identified handler.
 * 
 * This object itself takes a total of 16 or 32 bytes, depending on the version of the box. There
 * should be either zero or one instance of the {@code pitm} box.
 * 
 * This implementation follows the guide recommended in the Specification -
 * {@code ISO/IEC 14496-12:2015} on Page 80.
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
public class PrimaryItemBox extends FullBox
{
    private long itemID;

    /**
     * This constructor creates a derived Box object, extending the super class {@code FullBox} to
     * provide more specific information about this box.
     * 
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public PrimaryItemBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        itemID = (getVersion() == 0 ? reader.readUnsignedShort() : reader.readUnsignedInteger());

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns the identifier of the primary item.
     *
     * @return the Primary Item ID
     */
    public int getItemID()
    {
        return (int) itemID;
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

        line.append(String.format("\t%s '%s':\t\tItem_ID=%d", this.getClass().getSimpleName(), getBoxName(), getItemID()));

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
        line.append(String.format("  %-24s %s%n", "[Item ID]", itemID));

        return line.toString();
    }
}