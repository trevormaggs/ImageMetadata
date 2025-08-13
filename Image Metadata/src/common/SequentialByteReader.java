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
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class SequentialByteReader extends AbstractByteReader
{
    private long bufferIndex;
    private final Stack<Long> markPositionStack;

    /**
     * Constructs a reader for the given byte array with big-endian byte order.
     *
     * @param buf
     *        the byte array to read from
     */
    public SequentialByteReader(byte[] buf)
    {
        this(buf, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs a reader for the given byte array with a specified byte order.
     *
     * @param buf
     *        the byte array to read from
     * @param order
     *        the byte order to use
     */
    public SequentialByteReader(byte[] buf, ByteOrder order)
    {
        this(buf, 0L, order);
    }

    /**
     * Constructs a reader for the given byte array, starting from a specified offset.
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
     * Constructs a reader for the given byte array, starting from the specified offset in specified
     * byte order.
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
    public long getCurrentPosition()
    {
        return bufferIndex;
    }

    /**
     * Reads a single byte from the current position and advances the reader.
     *
     * @return the byte value
     */
    public byte readByte()
    {
        if (bufferIndex >= length())
        {
            throw new IndexOutOfBoundsException("End of buffer reached. Cannot read beyond position [" + length() + "]");
        }

        return getByte(bufferIndex++);
    }

    /**
     * Reads a sequence of bytes from the current position and advances the reader.
     *
     * @param length
     *        the number of bytes to read
     * 
     * @return a new byte array containing the read bytes
     */
    public byte[] readBytes(int length)
    {
        byte[] bytes = getBytes(bufferIndex, length);
        bufferIndex += length;

        return bytes;
    }

    /**
     * Reads an unsigned 8-bit integer from the current position and advances the reader.
     *
     * @return the unsigned byte value (0-255)
     */
    public short readUnsignedByte()
    {
        return (short) (readByte() & 0xFF);
    }

    /**
     * Reads a signed 16-bit integer from the current position and advances the reader.
     *
     * @return the short value
     */
    public short readShort()
    {
        short b1 = readUnsignedByte();
        short b2 = readUnsignedByte();

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            return (short) (b1 << 8 | b2);
        }

        else
        {
            return (short) (b2 << 8 | b1);
        }
    }

    /**
     * Reads an unsigned 16-bit integer from the current position and advances the reader.
     *
     * @return the unsigned short value (0-65535)
     */
    public int readUnsignedShort()
    {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads a signed 32-bit integer from the current position and advances the reader.
     *
     * @return The integer value.
     */
    public int readInteger()
    {
        int b1 = readUnsignedByte();
        int b2 = readUnsignedByte();
        int b3 = readUnsignedByte();
        int b4 = readUnsignedByte();

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            return (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
        }

        else
        {
            return (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        }

        // return readValue(4);
    }

    /**
     * Reads an unsigned 32-bit integer from the current position and advances the reader.
     *
     * @return the unsigned integer value as a long
     */
    public long readUnsignedInteger()
    {
        return readInteger() & 0xFFFFFFFFL;
    }

    /**
     * Reads a signed 64-bit long from the current position and advances the reader.
     *
     * @return the long value
     */
    public long readLong()
    {
        long b1 = readUnsignedByte();
        long b2 = readUnsignedByte();
        long b3 = readUnsignedByte();
        long b4 = readUnsignedByte();
        long b5 = readUnsignedByte();
        long b6 = readUnsignedByte();
        long b7 = readUnsignedByte();
        long b8 = readUnsignedByte();

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            return (b1 << 56) | (b2 << 48) | (b3 << 40) | (b4 << 32) | (b5 << 24) | (b6 << 16) | (b7 << 8) | b8;
        }

        else
        {
            return (b8 << 56) | (b7 << 48) | (b6 << 40) | (b5 << 32) | (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
        }
    }

    /**
     * Reads a 32-bit IEEE 754 floating-point value from the current position and advances the
     * reader.
     *
     * @return the float value
     */
    public float readFloat()
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Reads a 64-bit IEEE 754 floating-point value from the current position and advances the
     * reader.
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
     * 
     * @return the new position after skipping
     * 
     * @throws IndexOutOfBoundsException
     *         if skipping would exceed the buffer bounds
     */
    public long skip(long n)
    {
        long newPosition = bufferIndex + n;

        if (newPosition < 0 || newPosition > length())
        {
            throw new IndexOutOfBoundsException("Cannot skip by [" + n + "] bytes. New position [" + newPosition + "] is out of bounds [0, " + length() + "].");
        }

        bufferIndex = newPosition;

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
        if (pos < 0 || pos > length())
        {
            throw new IndexOutOfBoundsException("Position [" + pos + "] out of bounds. Valid range is [0 to " + length() + "].");
        }

        bufferIndex = pos;
    }

    /**
     * Marks the current position in the buffer, allowing a subsequent {@link #reset()} call to
     * return to this position.
     */
    public void mark()
    {
        markPositionStack.push(bufferIndex);
    }

    /**
     * Resets the reader's position to the last marked position.
     *
     * @throws IllegalStateException
     *         if the mark stack is empty
     */
    public void reset()
    {
        if (markPositionStack.isEmpty())
        {
            throw new IllegalStateException("Cannot reset position: mark stack is empty");
        }

        bufferIndex = markPositionStack.pop();
    }

    /**
     * Reads a null-terminated Latin-1 (ISO-8859-1) encoded string from the current position.
     *
     * <p>
     * The null terminator is consumed but not included in the returned string.
     * </p>
     *
     * @return the decoded string
     * 
     * @throws IllegalStateException
     *         if a null terminator is not found before the end of the buffer
     */
    public String readString()
    {
        long start = bufferIndex;
        long end = start;

        // Find the null terminator
        while (end < length())
        {
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

        // Read bytes and advance the index past the terminator
        byte[] stringBytes = getBytes(start, (int) (end - start));

        bufferIndex = end + 1;

        return new String(stringBytes, StandardCharsets.ISO_8859_1);
    }

    /**
     * Reads a signed integer of the specified byte length.
     *
     * @param numBytes
     *        number of bytes to read
     *        
     * @return the integer value
     */
    @SuppressWarnings("unused")
    private int readValue(int numBytes)
    {
        int value = 0;

        if (getByteOrder() == ByteOrder.BIG_ENDIAN)
        {
            for (int i = numBytes - 1; i >= 0; i--)
            {
                value |= (readUnsignedByte() << (i * 8));
            }
        }

        else
        {
            for (int i = 0; i < numBytes; i++)
            {
                value |= (readUnsignedByte() << (i * 8));
            }
        }

        return value;
    }
}