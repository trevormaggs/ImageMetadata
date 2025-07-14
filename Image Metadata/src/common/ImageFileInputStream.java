package common;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;

/**
 * A utility stream wrapper that simplifies reading binary image data from an {@link InputStream},
 * with support for both {@link ByteOrder#BIG_ENDIAN big-endian} and {@link ByteOrder#LITTLE_ENDIAN
 * little-endian} formats.
 * 
 * <p>
 * This class provides methods to read signed and unsigned values of various primitive types, for
 * example, {@code readShort()}, {@code readUnsignedInteger()} or {@code readFloat()}), while
 * tracking the current read position.
 * </p>
 *
 * <p>
 * <strong>Change History:</strong>
 * </p>
 * 
 * <ul>
 * <li>Version 1.0 - Initial release by Trevor Maggs on 21 June 2025</li>
 * </ul>
 *
 * @version 0.1
 * @since 21 June 2025
 * @author Trevor Maggs
 */

public class ImageFileInputStream implements AutoCloseable
{
    private long streamPosition;
    private ByteOrder byteOrder;
    private final InputStream stream;

    /**
     * Reads the next single byte of data from the stream and returns as an integer between 0 and
     * 255. If EOF is reached, a value of -1 is returned.
     * 
     * @return the value of the next byte in the stream, or a value of -1 if EOF is hit
     *
     * @throws IOException
     *         if an I/O error has occurred
     */
    private int readInternal() throws IOException
    {
        int byteValue = stream.read();

        if (byteValue != -1)
        {
            streamPosition++;
        }

        return byteValue;
    }

    /**
     * Reads up to {@code len} bytes from the stream into the buffer starting at the specified
     * offset.
     *
     * @param buf
     *        the destination byte array
     * @param offset
     *        the starting position in the buffer
     * @param len
     *        the number of bytes to read
     * 
     * @return the number of bytes read, or -1 if end of stream is reached
     * 
     * @throws IOException
     *         if an I/O error occurs
     * @throws NullPointerException
     *         if buf is null
     * @throws IndexOutOfBoundsException
     *         if offset or len are invalid
     */
    private int readInternal(byte[] buf, int offset, int len) throws IOException
    {
        if (buf == null)
        {
            throw new NullPointerException("Buffer is null");
        }

        else if (offset < 0 || len < 0 || len > buf.length - offset)
        {
            throw new IndexOutOfBoundsException("Byte array length is out of bound");
        }

        else if (len > 0)
        {
            int count = stream.read(buf, offset, len);

            if (count != -1)
            {
                streamPosition += count;
            }

            return count;
        }

        return 0;
    }

    /**
     * This default constructor should not be invoked, or it will throw an exception to prevent
     * instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    public ImageFileInputStream()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Constructs an {@code ImageFileInputStream} resource using the specified input stream and sets
     * the byte order to {@link ByteOrder#BIG_ENDIAN} by default.
     *
     * @param fis
     *        the input stream to wrap
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public ImageFileInputStream(InputStream fis) throws IOException
    {
        this(fis, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs an {@code ImageFileInputStream} resource using the specified input stream and byte
     * order.
     *
     * @param fis
     *        the input stream to wrap
     * @param order
     *        the byte order to use when interpreting multi-byte values, using either
     *        {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public ImageFileInputStream(InputStream fis, ByteOrder order) throws IOException
    {
        this.stream = fis;
        this.streamPosition = 0L;
        this.byteOrder = order;
    }

    /**
     * Specifies the required byte order that determines how byte values are read from the stream.
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
     * 
     * @see java.nio.ByteOrder for more details
     */
    public void setByteOrder(ByteOrder order)
    {
        byteOrder = order;
    }

    /**
     * Returns the current byte order used by this stream when reading multi-byte values.
     * 
     * The byte order determines how the stream interprets the sequence of bytes—either as
     * big-endian (most significant byte first) or little-endian (least significant byte first).
     *
     * @return the byte order used for reading, either {@link java.nio.ByteOrder#BIG_ENDIAN}
     *         or {@link java.nio.ByteOrder#LITTLE_ENDIAN}
     *
     * @see java.nio.ByteOrder for more details on byte order conventions
     */
    public ByteOrder getByteOrder()
    {
        return byteOrder;
    }

    /**
     * Returns the current byte position of the stream. The next read operation will take place
     * starting at this position.
     *
     * @return the position of the stream
     */
    public long getCurrentPosition()
    {
        return streamPosition;
    }

    /**
     * Reads a single byte from the stream and returns it as a signed byte value. Byte values
     * between 0x00 and 0x7F correspond to integer values between 0 and 127, while values between
     * 0x80 and 0xFF represent negative values ranging from -128 to -1.
     *
     * @return a signed byte value from the stream
     * 
     * @throws EOFException
     *         if the end of the stream is reached
     * @throws IOException
     *         if an I/O error occurs
     */
    public byte readByte() throws IOException
    {
        int ch = readInternal();

        if (ch < 0)
        {
            throw new EOFException();
        }

        return (byte) ch;
    }

    /**
     * Reads a single byte and returns it as an unsigned integer between 0 and 255.
     *
     * @return the unsigned byte value
     * 
     * @throws EOFException
     *         if the end of the stream is reached
     * @throws IOException
     *         if an I/O error occurs
     */
    public int readUnsignedByte() throws IOException
    {
        int ch = readInternal();

        if (ch < 0)
        {
            throw new EOFException();
        }

        return ch;
    }

    /**
     * Reads data from the input stream into an array of bytes. It attempts to read up to the
     * specified length, but may read fewer.
     * 
     * @param length
     *        the maximum number of bytes to read
     * 
     * @return an array of bytes that has been read
     * 
     * @throws IOException
     *         if there is a problem, such as an unacceptable length or an I/O error or if the
     *         stream is closed
     */
    public byte[] readBytes(int length) throws IOException
    {
        byte[] bytes = new byte[length];

        readInternal(bytes, 0, length);

        return bytes;
    }

    /**
     * Reads two bytes from the stream and returns a signed 16-bit short value, interpreted using
     * the current {@link ByteOrder}.
     *
     * @return the short value read from the stream
     * 
     * @throws EOFException
     *         if the stream ends prematurely
     * @throws IOException
     *         if an I/O error occurs
     */
    public short readShort() throws IOException
    {
        byte[] buf = new byte[2];

        if (readInternal(buf, 0, 2) != 2)
        {
            throw new EOFException();
        }

        return (short) ByteValueConverter.toShort(buf, byteOrder);
    }

    /**
     * Reads two bytes from the stream and returns the result as an unsigned short (16-bit) value,
     * based on the current byte order.
     *
     * @return an unsigned short value extracted from the stream
     * 
     * @throws EOFException
     *         if the stream ends prematurely
     * @throws IOException
     *         if an I/O error occurs
     */
    public int readUnsignedShort() throws IOException
    {
        return (int) (readShort() & 0xFFFF);
    }

    /**
     * Reads four bytes from the stream and returns a signed 32-bit integer value, interpreted using
     * the current {@link ByteOrder}.
     *
     * @return the integer value read from the stream
     * 
     * @throws EOFException
     *         if the stream ends prematurely
     * @throws IOException
     *         if an I/O error occurs
     */
    public int readInteger() throws IOException
    {
        byte[] buf = new byte[4];

        if (readInternal(buf, 0, 4) != 4)
        {
            throw new EOFException();
        }

        return ByteValueConverter.toInteger(buf, byteOrder);
    }

    /**
     * Reads four bytes from the stream and returns the result as an unsigned integer value, based
     * on the current byte order.
     *
     * @return a long value representing an unsigned integer
     * 
     * @throws EOFException
     *         if the stream ends prematurely
     * @throws IOException
     *         if an I/O error occurs
     */
    public long readUnsignedInteger() throws IOException
    {
        return ((long) readInteger()) & 0xFFFFFFFFL;
    }

    /**
     * Reads eight (8) bytes from the stream and returns a signed 64-bit long value, interpreted
     * using the current {@link ByteOrder}.
     *
     * @return the long value read from the stream
     * 
     * @throws EOFException
     *         if the stream ends prematurely
     * @throws IOException
     *         if an I/O error occurs
     */
    public long readLong() throws IOException
    {
        byte[] buf = new byte[8];

        if (readInternal(buf, 0, 8) != 8)
        {
            throw new EOFException();
        }

        return ByteValueConverter.toLong(buf, byteOrder);
    }

    /**
     * Reads 4 bytes from the stream and returns the result as a float value, based on the current
     * byte order.
     *
     * @return a float value from the stream
     *
     * @throws EOFException
     *         if the stream ends prematurely
     * @throws IOException
     *         if an I/O error occurs
     */
    public float readFloat() throws IOException
    {
        return Float.intBitsToFloat(readInteger());
    }

    /**
     * Reads 8 bytes from the stream and returns the result as a double value, based on the current
     * byte order.
     *
     * @return a double value from the stream
     *
     * @throws EOFException
     *         if the stream ends prematurely
     * @throws IOException
     *         if an I/O error occurs
     */
    public double readDouble() throws IOException
    {
        return Double.longBitsToDouble(readLong());
    }

    /**
     * Reads all bytes from the stream, copying into a new byte array and returns it.
     *
     * @return a byte array containing all copied data items
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    public byte[] readAllBytes() throws IOException
    {
        if (stream == null)
        {
            throw new NullPointerException("Input stream cannot be null");
        }

        int bytesRead;
        byte[] buffer = new byte[8192];

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            while ((bytesRead = stream.read(buffer)) != -1)
            {
                outputStream.write(buffer, 0, bytesRead);
                streamPosition += bytesRead;
            }

            outputStream.flush();

            return outputStream.toByteArray();
        }
    }

    /**
     * Skips up to {@code n} bytes in the stream and returns the number of bytes actually skipped.
     * 
     * This method ensures the requested number of bytes is skipped by repeatedly invoking
     * {@link InputStream#skip(long)} until complete, or until EOF is reached.
     * 
     * @param n
     *        the number of bytes to skip
     * 
     * @return the actual number of bytes skipped
     * 
     * @throws IOException
     *         if an I/O error occurs
     * @see <a href=
     *      "http://stackoverflow.com/questions/14057720/robust-skipping-of-data-in-a-java-io-inputstream-and-its-subtypes">Learn
     *      why this logic is necessary</a>
     */
    public long skip(long n) throws IOException
    {
        long count;
        long skippedTotal = 0L;

        if (n <= 0)
        {
            return 0;
        }

        while (skippedTotal < n)
        {
            count = stream.skip(n - skippedTotal);
            skippedTotal += count;

            if (count <= 0)
            {
                break;
            }
        }

        streamPosition += skippedTotal;

        return skippedTotal;
    }

    /**
     * Closes this input stream and releases any system resources associated with it.
     * 
     * Once closed, further read operations on this stream will throw an {@link IOException}. This
     * method invokes the underlying {@link InputStream}'s {@code close()} method.
     * 
     * @throws IOException
     *         if an I/O error occurs while closing the stream
     */
    @Override
    public void close() throws IOException
    {
        stream.close();
    }
}