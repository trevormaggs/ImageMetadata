package common;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Objects;

/**
 * A utility stream wrapper that simplifies reading binary image data using an {@link InputStream}
 * resource, with support for both {@link ByteOrder#BIG_ENDIAN big-endian} and
 * {@link ByteOrder#LITTLE_ENDIAN little-endian} formats.
 *
 * <p>
 * This class offers an ability to read signed and unsigned values of various primitive types, for
 * example: {@code readShort()}, {@code readUnsignedInteger()}, or {@code readFloat()}), while
 * tracking the current read position.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class ImageFileInputStream2 implements AutoCloseable
{
    private final BufferedInputStream stream;
    private ByteOrder byteOrder;
    private long streamPosition;

    /**
     * Constructs a reader for the specified input stream with big-endian byte order.
     *
     * @param fis
     *        the input stream to wrap. Must not be null
     */
    public ImageFileInputStream2(InputStream fis)
    {
        this(fis, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs a reader for the specified input stream with byte order provided.
     *
     * @param fis
     *        the input stream to wrap. Must not be null
     * @param order
     *        the byte order to use when interpreting multi-byte values
     * 
     * @throws NullPointerException
     *         if either input stream or byte order is null
     */
    public ImageFileInputStream2(InputStream fis, ByteOrder order)
    {
        if (fis == null)
        {
            throw new NullPointerException("InputStream cannot be null");
        }

        if (order == null)
        {
            throw new NullPointerException("Byte order cannot be null");
        }

        this.stream = new BufferedInputStream(fis);
        this.byteOrder = order;
        this.streamPosition = 0L;
    }

    /**
     * Sets the byte order used for interpreting multi-byte values.
     *
     * <p>
     * For instance, when reading the sequence of bytes '0x01 0x02 0x03 0x04' (which represents a
     * 4-byte integer), it would be interpreted as '0x01020304' in big-endian (network) byte order,
     * or '0x04030201' in little-endian byte order.
     * </p>
     *
     * <p>
     * A value of i{@code ByteOrder.BIG_ENDIAN} specifies big-endian or network byte order, where
     * the high-order byte comes first. Processors like Motorola and Sparc store data in this
     * format, while Intel processors use little-endian byte reverse order, represented by
     * {@code ByteOrder.LITTLE_ENDIAN}.
     * </p>
     *
     * @param order
     *        either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public void setByteOrder(ByteOrder order)
    {
        byteOrder = Objects.requireNonNull(order, "Byte order cannot be null");
    }

    /**
     * Returns the current byte order used by this stream.
     *
     * @return the byte order
     */
    public ByteOrder getByteOrder()
    {
        return byteOrder;
    }

    /**
     * Returns the current byte position in the stream.
     *
     * @return the current position
     */
    public long getCurrentPosition()
    {
        return streamPosition;
    }

    /**
     * Reads a single byte and returns it as a signed byte value.
     *
     * @return the signed byte value
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public byte readByte() throws IOException
    {
        int ch = stream.read();

        if (ch < 0)
        {
            throw new EOFException();
        }

        streamPosition++;

        return (byte) ch;
    }

    /**
     * Reads a single byte and returns it as an unsigned integer (0-255).
     *
     * @return the unsigned byte value
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public int readUnsignedByte() throws IOException
    {
        int ch = stream.read();

        if (ch < 0)
        {
            throw new EOFException();
        }

        streamPosition++;

        return ch;
    }

    /**
     * Reads a sequence of bytes from the stream.
     *
     * @param length
     *        The number of bytes to read
     * 
     * @return A new byte array containing the read bytes
     * 
     * @throws IOException
     *         if an I/O error occurs or the stream ends prematurely
     */
    public byte[] readBytes(int length) throws IOException
    {
        int bytesRead = 0;
        byte[] bytes = new byte[length];

        while (bytesRead < length)
        {
            int value = stream.read(bytes, bytesRead, length - bytesRead);

            if (value < 0)
            {
                throw new EOFException();
            }

            bytesRead += value;
        }

        streamPosition += length;

        return bytes;
    }

    /**
     * Reads two bytes and returns a signed 16-bit short value.
     *
     * @return the short value
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public short readShort() throws IOException
    {
        return ByteValueConverter.toShort(readBytes(2), byteOrder);
    }

    /**
     * Reads two bytes and returns an unsigned 16-bit short value as an integer.
     *
     * @return the unsigned short value
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public int readUnsignedShort() throws IOException
    {
        return readShort() & 0xFFFF;
    }

    /**
     * Reads four bytes and returns a signed 32-bit integer.
     *
     * @return the integer value
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public int readInteger() throws IOException
    {
        return ByteValueConverter.toInteger(readBytes(4), byteOrder);
    }

    /**
     * Reads four bytes and returns an unsigned 32-bit integer as a long.
     *
     * @return the unsigned integer value
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public long readUnsignedInteger() throws IOException
    {
        return readInteger() & 0xFFFFFFFFL;
    }

    /**
     * Reads eight bytes and returns a signed 64-bit long.
     *
     * @return the long value
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    public long readLong() throws IOException
    {
        return ByteValueConverter.toLong(readBytes(8), byteOrder);
    }

    /**
     * Reads four bytes and returns a 32-bit floating-point value.
     *
     * @return the float value
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Reads eight bytes and returns a 64-bit floating-point value.
     *
     * @return the double value
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads all remaining bytes from the stream into a new array.
     *
     * @return a byte array containing all remaining data
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public byte[] readAllBytes() throws IOException
    {
        int bytesRead;
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        while ((bytesRead = stream.read(buffer)) != -1)
        {
            baos.write(buffer, 0, bytesRead);
            streamPosition += bytesRead;
        }

        return baos.toByteArray();
    }

    /**
     * Peeks at the next byte in the stream without advancing the stream's position. The method
     * reads a single byte and then resets the stream to its original position.
     *
     * @return The next byte as an integer (0-255), or -1 if the end of the stream is reached
     * 
     * @throws IOException
     *         if an I/O error occurs or mark/reset is not supported
     */
    public int peekByte() throws IOException
    {
        // Mark the current position in the buffered stream.
        // The readlimit of 1 is sufficient since we only want to peek one byte.
        this.stream.mark(1);

        // Read the next byte. This is the "peek" operation.
        int peekedByte = this.stream.read();

        // Reset the stream to the marked position.
        // This effectively "un-reads" the byte, so the next read operation will get the same byte.
        this.stream.reset();

        return peekedByte;
    }

    /**
     * Peeks at a sequence of bytes at a specified offset from the current stream position, without
     * advancing the stream's position.
     *
     * @param offset
     *        The number of bytes to skip from the current position before peeking
     * @param length
     *        The number of bytes to read
     * 
     * @return A new byte array containing the peeked bytes
     * 
     * @throws IOException
     *         if an I/O error occurs, the stream ends prematurely, or the mark/reset operation is
     *         not supported
     */
    public byte[] peekBytes(int offset, int length) throws IOException
    {
        if (stream == null)
        {
            throw new IOException("Stream is not initialized.");
        }

        // The readlimit for the mark must be large enough to skip the offset and read the length.
        int readlimit = offset + length;

        if (readlimit < 0)
        {
            throw new IllegalArgumentException("Offset and length combined exceed integer max value.");
        }

        // Mark the current position.
        stream.mark(readlimit);

        // Skip to the desired offset.
        long skipped = stream.skip(offset);

        if (skipped < offset)
        {
            stream.reset(); // Always reset before throwing an exception to maintain state
            throw new IOException("Could not skip to the specified offset. Reached end of stream prematurely");
        }

        byte[] bytes = new byte[length];
        int bytesRead = stream.read(bytes, 0, length);

        // Reset the stream to the original marked position.
        stream.reset();

        if (bytesRead < length)
        {
            throw new IOException("Could not read all [" + length + "] bytes. Reached end of stream prematurely");
        }

        return bytes;
    }

    /**
     * Skips {@code n} bytes in the stream.
     *
     * @param n
     *        the number of bytes to skip
     * 
     * @return the actual number of bytes skipped
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public long skip(long n) throws IOException
    {
        long totalSkipped = 0;

        while (totalSkipped < n)
        {
            long skipped = stream.skip(n - totalSkipped);

            if (skipped == 0)
            {
                if (stream.read() == -1)
                {
                    // EOF
                    break;
                }

                skipped = 1;
            }

            totalSkipped += skipped;
        }

        streamPosition += totalSkipped;

        return totalSkipped;
    }

    /**
     * Closes the underlying input stream.
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    @Override
    public void close() throws IOException
    {
        stream.close();
    }
}