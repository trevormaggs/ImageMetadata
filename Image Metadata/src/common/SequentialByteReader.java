package common;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Stack;

/**
 * Performs sequential reading of primitive data types from a byte array.
 *
 * <p>
 * Supports reading of signed and unsigned integers, floating-point numbers, and byte sequences with
 * configurable byte order (big-endian or little-endian).
 * </p>
 *
 * <p>
 * Change History:
 * </p>
 * 
 * <ul>
 * <li>Version 1.0 – Initial release by Trevor Maggs on 21 June 2025</li>
 * </ul>
 *
 * @version 1.0
 * @since 21 June 2025
 */
public class SequentialByteReader extends AbstractByteReader
{
    private int bufferIndex;
    private final Stack<Integer> markPositionStack;

    /**
     * Constructs a reader for the given byte array starting from the beginning.
     *
     * @param buf
     *        the byte array to read from
     */
    public SequentialByteReader(byte[] buf)
    {
        this(buf, 0);
    }

    /**
     * Constructs a reader for the given byte array with the specified byte order.
     *
     * @param buf
     *        the byte array to read from
     * @param order
     *        the byte order to use
     */
    public SequentialByteReader(byte[] buf, ByteOrder order)
    {
        this(buf, 0, order);
    }

    /**
     * Constructs a reader for the given byte array starting from the specified offset.
     *
     * @param buf
     *        the byte array to read from
     * @param offset
     *        the starting position
     */
    public SequentialByteReader(byte[] buf, long offset)
    {
        this(buf, offset, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs a reader for the given byte array starting from the specified offset and using the
     * specified byte order.
     *
     * @param buf
     *        the byte array to read from
     * @param offset
     *        the starting position
     * @param order
     *        the byte order to use
     */
    public SequentialByteReader(byte[] buf, long offset, ByteOrder order)
    {
        super(buf, offset, order);
        this.bufferIndex = 0;
        this.markPositionStack = new Stack<>();
    }

    /**
     * Returns the current read position in the byte array.
     *
     * @return the current read position
     */
    public int getCurrentPosition()
    {
        return bufferIndex;
    }

    /**
     * Reads a single byte from the current position.
     *
     * @return the byte value
     */
    public byte readByte()
    {
        return getByte(bufferIndex++);
    }

    /**
     * Reads a sequence of bytes from the current position.
     *
     * @param length
     *        the number of bytes to read
     * @return a new byte array containing the read bytes
     */
    public byte[] readBytes(int length)
    {
        byte[] bytes = getBytes(bufferIndex, length);
        bufferIndex += length;
        return bytes;
    }

    /**
     * Reads an unsigned 8-bit integer from the current position.
     *
     * @return the unsigned byte value (0–255)
     */
    public short readUnsignedByte()
    {
        return (short) (readByte() & 0xFF);
    }

    /**
     * Reads a signed 16-bit integer from the current position.
     *
     * @return the short value
     */
    public short readShort()
    {
        byte b1 = readByte();
        byte b2 = readByte();

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
     * Reads an unsigned 16-bit integer from the current position.
     *
     * @return the unsigned short value (0–65535)
     */
    public int readUnsignedShort()
    {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads a signed 32-bit integer from the current position.
     *
     * @return the integer value
     */
    public int readInteger()
    {
        int b1 = readByte() & 0xFF;
        int b2 = readByte() & 0xFF;
        int b3 = readByte() & 0xFF;
        int b4 = readByte() & 0xFF;

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
     * Reads an unsigned 32-bit integer from the current position.
     *
     * @return the unsigned integer value as a long
     */
    public long readUnsignedInteger()
    {
        return readInteger() & 0xFFFFFFFFL;
    }

    /**
     * Reads a signed 64-bit long from the current position.
     *
     * @return the long value
     */
    public long readLong()
    {
        long b1 = readByte() & 0xFFL;
        long b2 = readByte() & 0xFFL;
        long b3 = readByte() & 0xFFL;
        long b4 = readByte() & 0xFFL;
        long b5 = readByte() & 0xFFL;
        long b6 = readByte() & 0xFFL;
        long b7 = readByte() & 0xFFL;
        long b8 = readByte() & 0xFFL;

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
     * Reads a 32-bit IEEE 754 floating-point value from the current position.
     *
     * @return the float value
     */
    public float readFloat()
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Reads a 64-bit IEEE 754 floating-point value from the current position.
     *
     * @return the double value
     */
    public double readDouble()
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Skips forward by the specified number of bytes.
     *
     * @param n
     *        the number of bytes to skip
     * @return the new position after skipping
     * @throws IndexOutOfBoundsException
     *         if skipping would exceed the buffer bounds
     */
    public long skip(long n)
    {
        if (bufferIndex + n > length())
        {
            throw new IndexOutOfBoundsException("Cannot skip beyond end of byte array. Requested: " + (bufferIndex + n) + ", Max: " + length());
        }

        bufferIndex += n;

        return bufferIndex;
    }

    /**
     * Moves to the specified position within the byte array.
     *
     * @param pos
     *        the position to move to
     * 
     * @throws IndexOutOfBoundsException
     *         if the position is invalid
     */
    public void seek(long pos)
    {
        if (pos >= 0 && pos <= length())
        {
            bufferIndex = (int) pos;
        }

        else
        {
            throw new IndexOutOfBoundsException("Position [" + pos + "] out of bounds. Valid range must be [0 to " + length() + "]");
        }
    }

    /**
     * Marks the current position in the buffer.
     */
    public void mark()
    {
        markPositionStack.push(bufferIndex);
    }

    /**
     * Resets to the last marked position.
     * 
     * @throws IllegalStateException
     *         if the mark stack is empty
     */
    public void reset()
    {
        if (!markPositionStack.isEmpty())
        {
            bufferIndex = markPositionStack.pop();
        }

        else
        {
            throw new IllegalStateException("Cannot reset position: mark stack is empty");
        }
    }

    /**
     * Reads a null-terminated Latin-1 (ISO-8859-1) encoded string from the current position.
     *
     * <p>
     * The null terminator is consumed but not included in the returned string.
     * </p>
     *
     * @return the decoded string
     */
    public String readString()
    {
        int start = bufferIndex;
        int end = start;

        while (end < length())
        {
            // Stops when the null terminator is hit
            if (getByte(end) == 0x00)
            {
                break;
            }

            end++;
        }

        if (end == length())
        {
            throw new IllegalStateException("Null terminator not found for string starting at position [" + start + "]");
        }

        // Copy bytes (excluding the null terminator)
        byte[] stringBytes = getBytes(start, end - start);

        // Advance bufferIndex past the null terminator
        bufferIndex = end + 1;

        return new String(stringBytes, StandardCharsets.ISO_8859_1);
    }
}