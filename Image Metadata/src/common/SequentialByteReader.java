package common;

import java.nio.ByteOrder;
import java.util.Stack;

/**
 * This class performs reader operations intended for obtaining data sequentially from a byte array
 * containing the raw data.
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
public class SequentialByteReader extends AbstractByteReader
{
    private int bufferIndex;
    private Stack<Integer> markPositionStack;

    /**
     * Constructs an instance to store the specified byte array containing the payload data, which
     * will be read from the beginning of the array.
     *
     * @param buf
     *        an array of bytes acting as the buffer for this instance
     */
    public SequentialByteReader(byte[] buf)
    {
        this(buf, 0);
    }

    /**
     * Constructs an instance to store the specified byte array, containing the payload data,
     * starting from the initial byte of the array. The specified byte order aids in interpreting
     * the input bytes correctly.
     *
     * @param buf
     *        an array of bytes acting as the buffer for this instance
     * @param order
     *        the byte order, either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public SequentialByteReader(byte[] buf, ByteOrder order)
    {
        this(buf, 0, order);
    }

    /**
     * Constructs an instance to store the byte array, which contains the payload data, beginning
     * from the specified offset in the array to read from.
     *
     * @param buf
     *        an array of bytes acting as the buffer for this instance
     * @param offset
     *        specifies the starting position within the specified array
     */
    public SequentialByteReader(byte[] buf, long offset)
    {
        this(buf, offset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs an instance to store the specified byte array containing the payload data and the
     * byte order aids in interpreting the input bytes correctly. The offset specifies the starting
     * position within the array to read from.
     *
     * @param buf
     *        an array of bytes acting as the buffer for this instance
     * @param offset
     *        specifies the starting position in the specified array
     * @param order
     *        the byte order, either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public SequentialByteReader(byte[] buf, long offset, ByteOrder order)
    {
        super(buf, offset, order);

        this.bufferIndex = 0;
        this.markPositionStack = new Stack<>();
    }

    /**
     * Returns the current position in the byte array.
     *
     * @return the position number
     */
    public int getCurrentPosition()
    {
        return bufferIndex;
    }

    /**
     * Returns a single byte from the current position in the byte array.
     * 
     * @return the byte of data at the current position
     */
    public byte readByte()
    {
        return getByte(bufferIndex++);
    }

    /**
     * Returns a sub-array from the current position in the byte array.
     *
     * @param length
     *        the number of bytes in the sub-array
     * 
     * @return a new byte array containing the specified subset of the array
     */
    public byte[] readBytes(int length)
    {
        byte[] bytes = getBytes(bufferIndex, length);

        bufferIndex += length;

        return bytes;
    }

    /**
     * Returns an unsigned 8-bit value from the current position in the byte array. The return value
     * is a short type to allow the unsigned byte to be returned without truncation.
     * 
     * @return the byte at the current position, giving the possible value range of 0 to 255
     */
    public short readUnsignedByte()
    {
        return (short) (readByte() & 0xFF);
    }

    /**
     * Reads two bytes from the array and returns the result as a short (16-bit) signed value, based
     * on the current byte ordering.
     * 
     * @return a signed short 2-byte value
     */
    public short readShort()
    {
        byte byte1 = readByte();
        byte byte2 = readByte();

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
     * Returns the result as an unsigned short 16-bit value, based on the current byte ordering. The
     * return value is an integer type to allow the unsigned short to be returned without loss of
     * precision.
     * 
     * @return an unsigned short (2 bytes) value
     */
    public int readUnsignedShort()
    {
        return ((int) readShort() & 0xFFFF);
    }

    /**
     * Reads four bytes from the array and returns the result as a signed integer (32-bit) value,
     * based on the current byte ordering.
     * 
     * @return an signed integer value
     */
    public int readInteger()
    {
        int byte1 = readByte() & 0xFF;
        int byte2 = readByte() & 0xFF;
        int byte3 = readByte() & 0xFF;
        int byte4 = readByte() & 0xFF;

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
     * Reads four bytes from the array and returns the result as an unsigned integer (32-bit) value,
     * based on the current byte ordering. The return value is a long type to allow the unsigned
     * integer to be returned without truncation.
     * 
     * @return an unsigned integer value
     */
    public long readUnsignedInteger()
    {
        return (long) (readInteger() & 0xFFFFFFFFL);
    }

    /**
     * Returns an 8-byte value as a signed long, based on the current byte ordering.
     * 
     * @return the 8-byte signed long value
     */
    public long readLong()
    {
        int byte1 = readByte() & 0xFF;
        int byte2 = readByte() & 0xFF;
        int byte3 = readByte() & 0xFF;
        int byte4 = readByte() & 0xFF;
        int byte5 = readByte() & 0xFF;
        int byte6 = readByte() & 0xFF;
        int byte7 = readByte() & 0xFF;
        int byte8 = readByte() & 0xFF;

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
     * @return a float value extracted from the byte array
     */
    public float getFloat32()
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Retrieves 8 bytes from the byte array and returns the result as a double value, based on the
     * current byte ordering.
     * 
     * @return a double value extracted from the byte array
     */
    public double getDouble64()
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Attempts to advance the position in the source byte array by the specified number of bytes.
     *
     * @param n
     *        the number of bytes to skip
     *
     * @return the updated position after skipping
     * @throws IndexOutOfBoundsException
     *         if the skipping leads to an out-of-bound position beyond the array limit
     */
    public long skip(long n)
    {
        if (bufferIndex + n > length())
        {
            throw new IndexOutOfBoundsException("Cannot skip beyond end of source byte array [Requested skip position: " + (bufferIndex + n) + ", Max position: " + (length() - 1) + "]");
        }

        bufferIndex += n;

        return bufferIndex;
    }

    /**
     * Advances the current byte position.
     *
     * @param pos
     *        the number of bytes to seek forward
     *
     * @throws IndexOutOfBoundsException
     *         if the specified position is out of bound
     */
    public void seek(long pos)
    {
        if (pos < length())
        {
            bufferIndex = (int) pos;
            return;
        }

        throw new IndexOutOfBoundsException("Specified position [" + pos + "] is out of bound. Must be within the range [" + length() + "]");
    }

    /**
     * Pushes the current byte position onto a stack of marked positions.
     */
    public void mark()
    {
        markPositionStack.push(bufferIndex);
    }

    /**
     * Resets the current byte position from the stack of marked positions.
     */
    public void reset()
    {
        if (markPositionStack.empty())
        {
            return;
        }

        bufferIndex = markPositionStack.pop();
        seek(bufferIndex);
    }

    // TEST FIRST
    public String readString()
    {
        byte b;
        StringBuilder sb = new StringBuilder(64);

        do
        {
            b = readByte();
            sb.append((char) b);

        } while (b != 0x00);

        return sb.toString();
    }
}