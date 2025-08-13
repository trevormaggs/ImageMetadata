package common;

import java.nio.ByteOrder;

/**
 * Performs random-access reading of primitive data types from a byte array.
 * 
 * <p>
 * Supports reading of signed and unsigned integers, floating-point values, and byte sequences, with
 * configurable byte order (big-endian or little-endian).
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class RandomAccessByteReader extends AbstractByteReader
{
    /**
     * Constructs an instance to read from the specified byte array, starting at index 0.
     *
     * @param buf
     *        the byte array to read from
     */
    public RandomAccessByteReader(byte[] buf)
    {
        this(buf, 0);
    }

    /**
     * Constructs an instance to read from the specified byte array, starting at index 0, using the
     * given byte order.
     *
     * @param buf
     *        the byte array to read from
     * @param order
     *        the byte order to use
     */
    public RandomAccessByteReader(byte[] buf, ByteOrder order)
    {
        this(buf, 0, order);
    }

    /**
     * Constructs an instance to read from the specified byte array, starting at the given offset.
     *
     * @param buf
     *        the byte array to read from
     * @param offset
     *        the starting index
     */
    public RandomAccessByteReader(byte[] buf, int offset)
    {
        this(buf, offset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs an instance to read from the specified byte array, starting at the given offset,
     * using the specified byte order.
     *
     * @param buf
     *        the byte array to read from
     * @param offset
     *        the starting index
     * @param order
     *        the byte order to use
     */
    public RandomAccessByteReader(byte[] buf, int offset, ByteOrder order)
    {
        super(buf, offset, order);
    }

    /**
     * Reads a single byte at the specified index.
     *
     * @param index
     *        the byte index
     * 
     * @return the byte value
     */
    public byte readByte(int index)
    {
        return getByte(index);
    }

    /**
     * Reads an unsigned 8-bit integer at the specified index.
     *
     * @param index
     *        the byte index
     * 
     * @return the unsigned value in the range [0, 255]
     */
    public short readUnsignedByte(int index)
    {
        return (short) (readByte(index) & 0xFF);
    }

    /**
     * Reads up to the specified length of bytes, starting at the given index.
     *
     * @param index
     *        the starting index
     * @param length
     *        the number of bytes to read
     * 
     * @return a new byte array containing the data
     */
    public byte[] readBytes(int index, int length)
    {
        return getBytes(index, length);
    }

    /**
     * Reads a signed 16-bit integer from the specified index.
     *
     * @param index
     *        the starting index
     * 
     * @return the signed short value
     */
    public short readShort(int index)
    {
        byte b1 = getByte(index);
        byte b2 = getByte(index + 1);

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            return (short) (((b1 & 0xFF) << 8) | (b2 & 0xFF));
        }

        else
        {
            return (short) (((b2 & 0xFF) << 8) | (b1 & 0xFF));
        }
    }

    /**
     * Reads an unsigned 16-bit integer from the specified index.
     *
     * @param index
     *        the starting index
     * 
     * @return the unsigned value in the range [0, 65535]
     */
    public int readUnsignedShort(int index)
    {
        return readShort(index) & 0xFFFF;
    }

    /**
     * Reads a signed 32-bit integer from the specified index.
     *
     * @param index
     *        the starting index
     * 
     * @return the signed integer value
     */
    public int readInteger(int index)
    {
        int b1 = getByte(index) & 0xFF;
        int b2 = getByte(index + 1) & 0xFF;
        int b3 = getByte(index + 2) & 0xFF;
        int b4 = getByte(index + 3) & 0xFF;

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
        }

        else
        {
            return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        }
    }

    /**
     * Reads an unsigned 32-bit integer from the specified index.
     *
     * @param index
     *        the starting index
     * 
     * @return the unsigned value as a long in the range [0, 2^32 - 1]
     */
    public long readUnsignedInteger(int index)
    {
        return readInteger(index) & 0xFFFFFFFFL;
    }

    /**
     * Reads a signed 64-bit integer from the specified index.
     *
     * @param index
     *        the starting index
     * 
     * @return the signed long value
     */
    public long readLong(int index)
    {
        long b1 = getByte(index) & 0xFFL;
        long b2 = getByte(index + 1) & 0xFFL;
        long b3 = getByte(index + 2) & 0xFFL;
        long b4 = getByte(index + 3) & 0xFFL;
        long b5 = getByte(index + 4) & 0xFFL;
        long b6 = getByte(index + 5) & 0xFFL;
        long b7 = getByte(index + 6) & 0xFFL;
        long b8 = getByte(index + 7) & 0xFFL;

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            return (b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) |
                    (b5 << 24) | (b6 << 16) | (b7 << 8) | b8;
        }

        else
        {
            return (b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) |
                    (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        }
    }

    /**
     * Reads a 32-bit IEEE 754 floating-point value from the specified index.
     *
     * @param index
     *        the starting index
     * 
     * @return the float value
     */
    public float readFloat(int index)
    {
        return Float.intBitsToFloat(readInteger(index));
    }

    /**
     * Reads a 64-bit IEEE 754 floating-point value from the specified index.
     *
     * @param index
     *        the starting index
     * 
     * @return the double value
     */
    public double readDouble(int index)
    {
        return Double.longBitsToDouble(readLong(index));
    }
}