package common;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
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
public class ImageFileInputStreamOrig implements AutoCloseable
{
    private final DataInputStream stream;
    private ByteOrder byteOrder;
    private long streamPosition;

    /**
     * Constructs a reader for the specified input stream with big-endian byte order.
     *
     * @param fis
     *        the input stream to wrap. Must not be null
     */
    public ImageFileInputStreamOrig(InputStream fis)
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
    public ImageFileInputStreamOrig(InputStream fis, ByteOrder order)
    {
        if (fis == null)
        {
            throw new NullPointerException("InputStream cannot be null");
        }

        if (order == null)
        {
            throw new NullPointerException("Byte order cannot be null");
        }

        this.stream = new DataInputStream(new BufferedInputStream(fis));
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
        byte b = stream.readByte();

        streamPosition++;

        return b;
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
        int b = stream.readUnsignedByte();

        streamPosition++;

        return b;
    }

    /**
     * Reads a sequence of bytes from the stream.
     *
     * @param length
     *        The number of bytes to read.
     * @return A new byte array containing the read bytes.
     * @throws IOException
     *         if an I/O error occurs or the stream ends prematurely.
     */
    public byte[] readBytes(int length) throws IOException
    {
        byte[] bytes = new byte[length];

        stream.readFully(bytes);
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
        short value = stream.readShort();

        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Short.reverseBytes(value);
        }

        streamPosition += 2;

        return value;
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
        int value = stream.readInt();

        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Integer.reverseBytes(value);
        }

        streamPosition += 4;

        return value;
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
        long value = stream.readLong();

        if (byteOrder == ByteOrder.LITTLE_ENDIAN)
        {
            value = Long.reverseBytes(value);
        }

        streamPosition += 8;

        return value;
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