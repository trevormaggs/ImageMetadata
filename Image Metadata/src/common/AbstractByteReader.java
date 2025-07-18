package common;

import java.nio.ByteOrder;
import java.util.Objects;

/**
 * This abstract class provides the functionality to perform reader operations intended for
 * obtaining data from a byte array. Data can either be read sequentially or at random, depending on
 * the implementing sub-classes.
 * 
 * <p>
 * Change History:
 * </p>
 * 
 * <ul>
 * <li>Version 1.0 - Initial release by Trevor Maggs on 21 June 2025</li>
 * </ul>
 * 
 * @version 0.1
 * @author Trevor Maggs, trevmaggs@tpg.com.au
 * @since 21 June 2025
 */
public abstract class AbstractByteReader
{
    private ByteOrder byteOrder;
    private final byte[] buffer;
    private final long baseOffset;

    /**
     * Constructs an instance to store the specified byte array containing payload data and the byte
     * order to interpret the input bytes correctly. The offset specifies the starting position
     * within the array to read from.
     *
     * @param buf
     *        an array of bytes acting as the buffer
     * @param offset
     *        specifies the starting position within the specified array
     * @param order
     *        the byte order, either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public AbstractByteReader(byte[] buf, long offset, ByteOrder order)
    {
        if (offset < 0)
        {
            throw new IllegalArgumentException("Base offset cannot be less than zero. Detected offset: [" + offset + "]");
        }

        this.buffer = Objects.requireNonNull(buf, "Input buffer cannot be null");
        this.baseOffset = offset;
        this.byteOrder = order;
    }

    /**
     * Checks whether the specified position is within the byte array's bounds. If the position is
     * out of range, an {@code IndexOutOfBoundsException} is thrown.
     *
     * @param position
     *        the position in the byte array
     * @param length
     *        the total length within the byte array to check
     * 
     * @throws IndexOutOfBoundsException
     *         if the position is out of bounds
     */
    private void validateByteIndex(long position, int length)
    {
        if (position < 0)
        {
            throw new IndexOutOfBoundsException("Cannot read the buffer with a negative index [" + position + "]");
        }

        if (length < 0)
        {
            throw new IndexOutOfBoundsException("Length of requested bytes cannot be negative [" + length + "]");
        }

        if ((baseOffset + position + length - 1) >= buffer.length)
        {
            throw new IndexOutOfBoundsException(String.format("Attempt to read beyond end of buffer [baseOffset: %d, index: %d, requestedLength: %d, bufferLength: %d]", baseOffset, position, length, buffer.length));
        }
    }

    /**
     * Returns a single byte from the array at the specified position.
     *
     * @param position
     *        the index in the byte array from where the data should be returned
     *
     * @return the byte at the specified position
     */
    protected byte getByte(long position)
    {
        validateByteIndex(baseOffset + position, 1);

        return buffer[(int) (baseOffset + position)];
    }

    /**
     * Copies and returns a sub-array from the byte array, starting from the specified position.
     * 
     * @param position
     *        the index within the byte array from which to start copying
     * @param length
     *        the total number of bytes to include in the sub-array
     * 
     * @return a new byte array containing the specified subset of the original array
     */
    protected byte[] getBytes(long position, int length)
    {
        validateByteIndex(position, length);

        byte[] bytes = new byte[length];
        int sourcePos = (int) (baseOffset + position);

        System.arraycopy(buffer, sourcePos, bytes, 0, length);

        return bytes;
    }

    /**
     * Checks if there are more bytes available to read.
     *
     * @param position
     *        the current read index within the byte array
     * 
     * @return true if the buffer has remaining unread bytes, false otherwise
     */
    public boolean hasRemaining(long position)
    {
        return position < length();
    }

    /**
     * Retrieves the offset pointer to the byte array, where read operations can start from.
     *
     * @return the base offset
     */
    public long getBaseOffset()
    {
        return baseOffset;
    }

    /**
     * Sets the byte order for interpreting the input bytes correctly, based on either the Motorola
     * big endian-ness or Intel little endian-ness format.
     * 
     * Use {@code ByteOrder.BIG_ENDIAN} for image files following the Motorola endian-ness order,
     * where the Most Significant Bit (MSB) precedes the Least Significant Bit (LSB). This order is
     * also referred to as network byte order.
     * 
     * Use {@code ByteOrder.LITTLE_ENDIAN} for image files following Intel's little-endian order. In
     * contrast to Motorola's byte order, the LSB comes before the MSB.
     * 
     * @param order
     *        the byte order for interpreting the input bytes, either {@code ByteOrder.BIG_ENDIAN}
     *        (Motorola) or {@code ByteOrder.LITTLE_ENDIAN} (Intel)
     */
    public void setByteOrder(ByteOrder order)
    {
        byteOrder = order;
    }

    /**
     * Returns the byte order, indicating how data values will be interpreted correctly.
     *
     * @return either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     *
     * @see java.nio.ByteOrder for more details
     */
    public ByteOrder getByteOrder()
    {
        return byteOrder;
    }

    /**
     * Returns the length of the byte array minus the initial offset length.
     *
     * @return the array length
     */
    public long length()
    {
        return buffer.length - baseOffset;
    }

    /**
     * Retrieves the data at the specified offset within the byte array without advancing the
     * position.
     * 
     * <p>
     * A call to this method is equivalent to {@code getByte(position)}, but does not advance any
     * position counters.
     * </p>
     * 
     * @param offset
     *        the offset from the beginning of the byte array to fetch the data
     * 
     * @return the byte of data
     */
    public byte peek(long offset)
    {
        return getByte(offset);
    }

    /**
     * Retrieves up to the specified length of a sub-array of bytes at the specified offset without
     * advancing the position of the original array.
     * 
     * <p>
     * A call to this method is equivalent to {@code getByte(position, length)}, but does not
     * advance any position counters.
     * </p>
     * 
     * @param offset
     *        the offset from the beginning of the byte array to fetch the data
     * @param length
     *        the total number of bytes to include in the sub-array
     * 
     * @return the byte containing the data
     */
    public byte[] peek(long offset, int length)
    {
        return getBytes(offset, length);
    }

    /**
     * Primarily intended for debugging purposes, it prints out a series of raw byte values.
     */
    public void printRawBytes()
    {
        for (int i = 0; i < buffer.length; i++)
        {
            if (i % 16 == 0)
            {
                System.out.println();
                System.out.printf("%04X: ", i);
            }

            else if (i % 16 == 8)
            {
                System.out.print("- ");
            }

            System.out.printf("%02X ", buffer[i]);
        }

        System.out.println();

        System.out.printf("buffer length: %d%s", buffer.length, System.lineSeparator());
    }
}