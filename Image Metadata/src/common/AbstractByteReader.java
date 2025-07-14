package common;

import java.nio.ByteOrder;

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
        if (buf == null)
        {
            throw new NullPointerException("Input buffer cannot be null");
        }

        if (offset < 0)
        {
            throw new IllegalArgumentException("Base offset cannot be less than zero. Detected offset: [" + offset + "]");
        }

        this.buffer = buf;
        this.baseOffset = offset;
        this.byteOrder = order;
    }

    /**
     * Verifies the specified position falls within the bounds of the byte array.
     *
     * @param index
     *        the position in the byte array
     * @param length
     *        the total length within the byte array to check
     * 
     * @return a boolean true value if the position falls within the array range, and false
     *         otherwise
     */
    private boolean validateByteIndex(long index, long length)
    {
        return (length >= 0 && index >= 0) && ((index + length - 1) < length());
    }

    /**
     * Checks whether the specified position is within the byte array's bounds. If the position is
     * out of range, an {@code IndexOutOfBoundsException} is thrown.
     *
     * @param index
     *        the position in the byte array
     * @param length
     *        the total length within the byte array to check
     * 
     * @throws IndexOutOfBoundsException
     *         if the position is out of bounds
     */
    private void checkPositionIndex(long index, int length)
    {
        if (!validateByteIndex(index, length))
        {
            String msg;

            if (index < 0)
            {
                msg = String.format("Cannot read the buffer with a negative index [%d]", index);
            }

            else if (length < 0)
            {
                msg = String.format("Length of requested bytes cannot be negative [%d]", length);
            }

            else if (index + length - 1 > Integer.MAX_VALUE)
            {
                msg = String.format("Index is out of bounds for the signed 32-bit integer range. Must be less than [" + length() + "]. Found [" + (index + length) + "]");
            }

            else
            {
                msg = String.format("Attempt to read beyond the end of underlying data source [Index: %d, requested length: %d, max index: %d]", index, length, buffer.length - 1);
            }

            throw new IndexOutOfBoundsException(msg);
        }
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
     * position. A call to this method is equal to the returned value of
     * {@code getByte(int position)} defined in the superclass.
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
     * advancing the position of the original array. A call to this method is equal to the next
     * return of {@code getBytes(int position, int length)}.
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
        System.out.printf("buffer length: %d\n", buffer.length);
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
        checkPositionIndex(baseOffset + position, 1);

        return buffer[(int) (baseOffset + position)];
    }

    /**
     * Copies and returns a sub-array from the byte array, starting from the specified position.
     *
     * @param position
     *        the index within the byte array from where the bytes should be returned
     * @param length
     *        the total number of bytes to include in the sub-array
     *
     * @return a new byte array containing the specified subset of the original array
     */
    protected byte[] getBytes(long position, int length)
    {
        byte[] bytes;
        int sourcePos = (int) (baseOffset + position);

        checkPositionIndex(sourcePos, length);
        bytes = new byte[length];
        System.arraycopy(buffer, sourcePos, bytes, 0, length);

        return bytes;
    }
}