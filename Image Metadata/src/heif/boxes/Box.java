package heif.boxes;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import common.ByteValueConverter;
import common.SequentialByteReader;
import heif.HeifBoxType;
import logger.LogFactory;

/**
 * Represents a generic HEIF Box, according to ISO/IEC 14496-12:2015. Handles both standard boxes
 * and {@code uuid} extended boxes.
 */
public class Box
{
    private static final LogFactory LOGGER = LogFactory.getLogger(Box.class);
    private static final long BOX_SIZE_TO_EOF = Long.MAX_VALUE;
    private final ByteOrder order;
    private final long boxSize;
    private final byte[] boxTypeBytes;
    private final String userType;
    private final HeifBoxType type;
    protected long byteUsed;
    private Box parent;

    /**
     * Constructs a {@code Box} by reading its header from the specified
     * {@code SequentialByteReader}.
     *
     * @param reader
     *        the byte reader for parsing
     */
    public Box(SequentialByteReader reader)
    {
        long startPos = reader.getCurrentPosition();

        this.order = reader.getByteOrder();
        long sizeField = reader.readUnsignedInteger();
        this.boxTypeBytes = reader.readBytes(4);
        this.type = HeifBoxType.fromTypeBytes(boxTypeBytes);

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
        this.parent = box.parent;
    }

    /**
     * Sets the parent of this child box.
     *
     * @param parent
     *        the Box referencing to the parent box
     */
    public void setParent(Box parent)
    {
        this.parent = parent;
    }

    /**
     * Returns the parent box of this child box for referencing purposes
     *
     * @return the Box reference
     */
    public Box getParent()
    {
        return parent;
    }

    /**
     * Returns the depth of this box within the box hierarchy.
     * The root box has a depth of 0; each level below increases the depth by 1.
     *
     * @return the depth of this box in the hierarchy
     */
    public int getHierarchyDepth()
    {
        int depth = 0;

        for (Box p = getParent(); p != null; p = p.getParent())
        {
            depth++;
        }

        return depth;
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
     * @throws IllegalStateException
     *         if malformed data is encountered. The box size is unknown (extends to EOF)
     */
    public int available()
    {
        if (boxSize == BOX_SIZE_TO_EOF)
        {
            throw new IllegalStateException("Box size is unknown (extends to EOF). Remaining size cannot be calculated");
        }

        return (int) (boxSize - byteUsed);
    }

    /**
     * Returns the number of bytes already read from this box.
     *
     * @return bytes read so far
     */
    public long byteUsed()
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
     * Logs a single diagnostic line for this box at the debug level.
     *
     * <p>
     * This is useful when traversing the box tree of a HEIF/ISO-BMFF structure for debugging or
     * inspection purposes.
     * </p>
     */
    public void logBoxInfo()
    {
        String tab = repeatPrint("\t", getHierarchyDepth());
        LOGGER.debug(String.format("%s'%s':\t\t\t%s", tab, getTypeAsString(), type.getTypeName()));
    }

    /**
     * Generates a line of padded characters to n of times.
     * 
     * @param subject
     *        string to be padded
     * @param n
     *        number of times to pad in integer form
     * 
     * @return formatted string
     */
    protected static String repeatPrint(String ch, int n)
    {
        if (n == 0)
        {
            return "";
        }

        else if (n > 0)
        {
            ch = ch + repeatPrint(ch, n - 1);
        }

        return ch;
    }
}
