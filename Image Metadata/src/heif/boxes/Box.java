package heif.boxes;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import heif.HeifBoxType;
import heif.HeifBoxType.BoxCategory;
import common.ByteValueConverter;
import common.SequentialByteReader;

public class Box
{
    private ByteOrder order;
    private long boxsize;
    private byte[] boxtype;
    private String usertype;
    private HeifBoxType type;
    protected int byteUsed;

    /**
     * This implementation follows the guide recommended in the Specification - ISO/IEC
     * 14496-12:2015
     * {@code Information technology — Coding of audiovisual objects. Part 12: ISO base media file format}.
     * Refer to Page 6 for exact details.
     * 
     * This constructor creates a new Box object to read the box header, providing crucial
     * information on data length, box type and other relevant information.
     * 
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public Box(SequentialByteReader reader)
    {
        int pos = reader.getCurrentPosition();

        this.order = reader.getByteOrder();
        this.boxsize = reader.readUnsignedInteger();
        this.boxtype = reader.readBytes(4);
        this.type = HeifBoxType.getBoxType(boxtype);

        if (boxsize == 1)
        {
            // TODO: need to test for completeness. Likely when it hits mdat boxes
            boxsize = reader.readLong();
        }

        else if (boxsize == 0)
        {
            // TODO: Make a decision how this logic is best managed.
            // This is when the box is the last one in the stream.
            boxsize = -1;
        }

        if (getBoxName().equals("uuid"))
        {
            usertype = new String(reader.readBytes(16), StandardCharsets.UTF_8);
        }

        byteUsed = reader.getCurrentPosition() - pos;
    }

    /**
     * A copy constructor to replicate field values one by one, useful when aiming to subclass,
     * retaining the original field values.
     * 
     * @param box
     *        a live Box object whose field data is copied
     */
    public Box(Box box)
    {
        this.boxsize = box.boxsize;
        this.boxtype = box.boxtype;
        this.usertype = box.usertype;
        this.byteUsed = box.byteUsed;
        this.order = box.order;
        this.type = box.type;
    }

    /**
     * Returns the byte order, assuring the correct interpretation of data values. For HEIC files,
     * the big-endian-ness is the standard configuration.
     *
     * @return either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public ByteOrder getByteOrder()
    {
        return order;
    }

    /**
     * Returns the type of this Box object as a string.
     *
     * @return the box type in textual form
     */
    public String getBoxName()
    {
        return new String(boxtype, StandardCharsets.UTF_8);
    }

    /**
     * Returns the size of this Box object. The size may increase if this class is extended to a
     * subclass.
     *
     * @return The size of this box
     */
    public int getBoxSize()
    {
        return (int) boxsize;
    }

    /**
     * Returns the remaining or unused bytes for this box, after the initial bytes have been read.
     *
     * @return the size of the remaining bytes
     */
    public int remainingBytes()
    {
        return (int) (boxsize - byteUsed);
    }

    /**
     * Returns the count of bytes already consumed through reading.
     *
     * @return the number of processed bytes
     */
    public int byteUsed()
    {
        return byteUsed;
    }

    /**
     * Returns the user type.
     *
     * @return a string
     */
    public String getUserType()
    {
        return (usertype == null ? "" : usertype);
    }

    /**
     * Returns a boolean value, indicating if this box is a container type.
     *
     * @return true if this box is a container
     */
    public boolean isContainerType()
    {
        return (HeifBoxType.getBoxType(getBoxName()).getBoxCategory() == BoxCategory.CONTAINER);
    }

    /**
     * Gets the type of this box in the context of the HeifBoxType enumeration value.
     *
     * @return the HeifBoxType value
     */
    public HeifBoxType getHeifType()
    {
        return type;
    }

    /**
     * Convert this box type into a representative number for identification purposes.
     *
     * @return the ID number in an integer form
     */
    public int getTypeIdentifier()
    {
        return ByteValueConverter.toInteger(boxtype, order);
    }

    /**
     * Displays a list of structured references associated with the specified HEIF based file,
     * useful for analytical purposes.
     *
     * @return the string
     */
    public String showBoxStructure()
    {
        StringBuilder line = new StringBuilder();

        line.append(String.format("\t\t\t'%s':\t\t\t%s", getBoxName(), HeifBoxType.getBoxType(getBoxName()).getBoxName()));

        return line.toString();
    }

    /**
     * XXX
     *
     * @return
     */
    public List<Box> addBoxList()
    {
        return null;
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

        line.append(String.format("  %-24s %s%n", "[Box Length]", getBoxSize()));
        line.append(String.format("  %-24s %s%n", "[Box Type]", getBoxName()));
        line.append(String.format("  %-24s %s%n", "[Container]", isContainerType() ? "Yes" : "No"));
        line.append(String.format("  %-24s %s%n", "[Bytes used]", byteUsed));

        if (getUserType().length() > 0)
        {
            line.append(String.format("  %-24 %s%n", "[User Type]", getUserType()));
        }

        return line.toString();
    }
}