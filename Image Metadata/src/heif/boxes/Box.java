package heif.boxes;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import common.ByteValueConverter;
import common.SequentialByteReader;
import heif.HeifBoxType;
import heif.HeifBoxType.BoxCategory;

/**
 * Represents a generic HEIF Box, according to ISO/IEC 14496-12:2015. Handles both standard boxes
 * and {@code uuid} extended boxes.
 */
public class Box
{
    private static final long BOX_SIZE_TO_EOF = Long.MAX_VALUE;

    private final ByteOrder order;
    private final long boxSize;
    private final byte[] boxTypeBytes;
    private final String userType;
    private final HeifBoxType type;

    protected int byteUsed;

    /**
     * Constructs a {@code Box} by reading its header from the specified
     * {@code SequentialByteReader}.
     *
     * @param reader
     *        the byte reader for parsing
     */
    public Box(SequentialByteReader reader)
    {
        int startPos = reader.getCurrentPosition();

        this.order = reader.getByteOrder();
        long sizeField = reader.readUnsignedInteger();
        this.boxTypeBytes = reader.readBytes(4);
        this.type = HeifBoxType.getBoxType(boxTypeBytes);

        if (sizeField == 1)
        {
            this.boxSize = reader.readLong();
        }

        else if (sizeField == 0)
        {
            this.boxSize = BOX_SIZE_TO_EOF;
        }

        else
        {
            this.boxSize = sizeField;
        }

        if (type == HeifBoxType.UUID)
        {
            byte[] uuidBytes = reader.readBytes(16);
            this.userType = ByteValueConverter.toHex(uuidBytes);
        }

        else
        {
            this.userType = null;
        }

        this.byteUsed = reader.getCurrentPosition() - startPos;
    }

    /**
     * A copy constructor to replicate field values one by one, useful for sub-classing, retaining
     * the original field values.
     *
     * @param box
     *        the box to copy
     */
    public Box(Box box)
    {
        this.order = box.order;
        this.boxSize = box.boxSize;
        this.boxTypeBytes = box.boxTypeBytes.clone();
        this.userType = box.userType;
        this.type = box.type;
        this.byteUsed = box.byteUsed;
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
     * Returns the 4-character box type as a string.
     *
     * @return the box type in textual form
     */
    public String getTypeAsString()
    {
        return new String(boxTypeBytes, StandardCharsets.US_ASCII);
    }

    /**
     * Returns the total size of this box, or {@link Long#MAX_VALUE} if size is unknown.
     *
     * @return the box size
     */
    public long getBoxSize()
    {
        return boxSize;
    }

    /**
     * Returns the number of remaining bytes in the box.
     *
     * @return remaining bytes
     * 
     * @throws UnsupportedOperationException
     *         if the box size is unknown (extends to EOF)
     */
    public int available()
    {
        if (boxSize == BOX_SIZE_TO_EOF)
        {
            throw new UnsupportedOperationException("Box size is unknown (extends to EOF). Remaining size cannot be calculated");
        }

        return (int) (boxSize - byteUsed);
    }

    /**
     * Returns the number of bytes already read from this box.
     *
     * @return bytes read so far
     */
    public int byteUsed()
    {
        return byteUsed;
    }

    /**
     * Returns the user type for a {@code uuid} box, or an empty Optional if not applicable.
     *
     * @return optional user type
     */
    public Optional<String> getUserType()
    {
        return Optional.ofNullable(userType);
    }

    /**
     * Returns whether this box is a container box.
     *
     * @return true if this box is a container
     */
    public boolean isContainerType()
    {
        return type.getBoxCategory() == BoxCategory.CONTAINER;
    }

    /**
     * Returns the {@link HeifBoxType} of this box.
     *
     * @return the type
     */
    public HeifBoxType getHeifType()
    {
        return type;
    }

    /**
     * Returns a list of child boxes, if applicable. Default is empty.
     *
     * @return list of contained boxes
     */
    public List<Box> getBoxList()
    {
        return Collections.emptyList();
    }

    /**
     * Returns a concise string representation of this box.
     *
     * @return formatted string
     */
    @Override
    public String toString()
    {
        return toString(null);
    }

    /**
     * Returns a detailed debug string for this box.
     *
     * @param prefix
     *        optional prefix to prepend
     * @return formatted string
     */
    public String toString(String prefix)
    {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isEmpty())
        {
            sb.append(prefix).append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }

        sb.append(String.format("\t\t\t'%s':\t\t\t%s", getTypeAsString(), type.getTypeName()));

        return sb.toString();
    }
}