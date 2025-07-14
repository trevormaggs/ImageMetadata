package common;

import java.nio.ByteOrder;

/**
 * This class performs reader operations intended for obtaining data from a byte array at random.
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
public class RandomAccessByteReader extends AbstractByteReader
{
    /**
     * Constructs an instance to store the specified byte array containing the payload data to be
     * read from the beginning of the array.
     *
     * @param buf
     *        an array of bytes that acts as a buffer within this instance
     */
    public RandomAccessByteReader(byte[] buf)
    {
        this(buf, 0);
    }

    /**
     * Constructs an instance to store the byte array containing payload data and the byte order to
     * interpret the input bytes correctly. The data will be read from the beginning of the array.
     *
     * @param buf
     *        an array of bytes acting as the buffer for this instance
     * @param order
     *        the byte order, either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public RandomAccessByteReader(byte[] buf, ByteOrder order)
    {
        this(buf, 0, order);
    }

    /**
     * Constructs an instance to store a byte array that contains payload or raw data. The data will
     * be read from the specified offset within the array.
     *
     * @param buf
     *        an array of bytes that acts as a buffer for this instance
     * @param offset
     *        specifies the starting position within the specified array
     */
    public RandomAccessByteReader(byte[] buf, int offset)
    {
        this(buf, offset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs an instance to store the specified byte array containing payload data and the byte
     * order to interpret the input bytes correctly. The offset specifies the starting position
     * within the array to be read from.
     *
     * @param buf
     *        an array of bytes acting as the buffer for this instance
     * @param offset
     *        specifies the starting position within the specified array
     * @param order
     *        the byte order, either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public RandomAccessByteReader(byte[] buf, int offset, ByteOrder order)
    {
        super(buf, offset, order);
    }

    /**
     * Reads a single byte at the specified position in the byte array.
     * 
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return the byte at the specified position
     */
    public byte readByte(int index)
    {
        return getByte(index);
    }

    /**
     * Reads an unsigned 8-bit integer at the specified position in the byte array. The returned
     * value is a short type to allow the unsigned byte to be returned without any form of data
     * corruption.
     *
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return the byte at the specified position, which can have a value ranging from 0 to 255
     */
    public short readUnsignedByte(int index)
    {
        return (short) (readByte(index) & 0xFF);
    }

    /**
     * Reads up to the length of a sub-array starting at the specified position in the byte array.
     *
     * @param index
     *        the position of the first byte in the sub-array
     * @param length
     *        the total number of bytes in the sub-array
     *
     * @return a new byte array containing the specified subset of the original array
     */
    public byte[] readBytes(int index, int length)
    {
        return getBytes(index, length);
    }

    /**
     * Reads two bytes at the specified position and returns the result as a short (16-bit) signed
     * value, based on the current byte ordering.
     * 
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return a signed short 2-byte value
     */
    public short readShort(int index)
    {
        byte byte1 = getByte(index);
        byte byte2 = getByte(index + 1);

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            return (short) ((byte1 << 8) & 0xFF00 | byte2 & 0xFF);
        }

        else
        {
            return (short) ((byte2 << 8) & 0xFF00 | byte1 & 0xFF);
        }
    }

    /**
     * Reads two bytes from the specified position and returns the result as an unsigned value,
     * based on the current byte ordering. The return value is an integer type to allow the unsigned
     * short to be returned without truncation.
     * 
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return an unsigned integer value
     */
    public int readUnsignedShort(int index)
    {
        return ((int) readShort(index) & 0xFFFF);
    }

    /**
     * Reads four bytes at the specified position and returns the result as a signed integer
     * (32-bit) value, based on the current byte ordering.
     * 
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return a signed integer value
     */
    public int readInteger(int index)
    {
        int byte1 = getByte(index) & 0xFF;
        int byte2 = getByte(index + 1) & 0xFF;
        int byte3 = getByte(index + 2) & 0xFF;
        int byte4 = getByte(index + 3) & 0xFF;

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            return (byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4);
        }

        else
        {
            return (byte4 << 24 | byte3 << 16 | byte2 << 8 | byte1);
        }
    }

    /**
     * Reads four bytes at the specified position and returns the result as an unsigned value, based
     * on the current byte ordering. The return value is a long type to allow the unsigned integer
     * to be returned without truncation.
     * 
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return an unsigned long value
     */
    public long readUnsignedInteger(int index)
    {
        return (long) readInteger(index) & 0xFFFFFFFFL;
    }

    /**
     * Returns an 8-byte value as a signed long, based on the current byte ordering.
     * 
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return an 8-byte signed long value
     */
    public long readLong(int index)
    {
        int byte1 = getByte(index) & 0xFF;
        int byte2 = getByte(index + 1) & 0xFF;
        int byte3 = getByte(index + 2) & 0xFF;
        int byte4 = getByte(index + 3) & 0xFF;
        int byte5 = getByte(index + 4) & 0xFF;
        int byte6 = getByte(index + 5) & 0xFF;
        int byte7 = getByte(index + 6) & 0xFF;
        int byte8 = getByte(index + 7) & 0xFF;

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            // Motorola - MSB first
            return (long) (byte1 << 56 | byte2 << 48 | byte3 << 40 | byte4 << 32 |
                    byte5 << 24 | byte6 << 16 | byte7 << 8 | byte8);
        }

        else
        {
            // Intel ordering - LSB first
            return (long) (byte8 << 56 | byte7 << 48 | byte6 << 40 | byte5 << 32 |
                    byte4 << 24 | byte3 << 16 | byte2 << 8 | byte1);
        }
    }

    /**
     * Retrieves 4 bytes from the byte array and returns the result as a float value, based on the
     * current byte ordering.
     * 
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return a float value extracted from the byte array
     */
    public float getFloat32(int index)
    {
        return Float.intBitsToFloat(readInteger(index));
    }

    /**
     * Retrieves 8 bytes from the byte array and returns the result as a double value, based on the
     * current byte ordering.
     * 
     * @param index
     *        the position of the first byte in the sub-array
     * 
     * @return a double value extracted from the byte array
     */
    public double getDouble64(int index)
    {
        return Double.longBitsToDouble(readLong(index));
    }
}