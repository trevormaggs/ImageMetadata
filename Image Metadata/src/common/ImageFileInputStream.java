package common;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility for reading binary data from image files with configurable byte order.
 *
 * <p>
 * This class wraps an {@link InputStream} and provides methods to read signed and unsigned values
 * of various primitive types, for example: {@code readShort()}, {@code readUnsignedInteger()}, or
 * {@code readFloat()}, while keeping track of the current read position. Both big-endian and
 * little-endian formats are supported.
 * </p>
 * 
 * <p>
 * The byte order can be changed at any time via {@link #setByteOrder(ByteOrder)}.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class ImageFileInputStream implements AutoCloseable
{
    private final DataInputStream stream;
    private ByteOrder byteOrder;
    private long streamPosition;

    /**
     * Constructs a reader for the specified input stream with byte order provided.
     *
     * @param fin
     *        the input stream to wrap. Must not be null
     * @param order
     *        the byte order to use when interpreting multi-byte values
     * 
     * @throws NullPointerException
     *         if either input stream or byte order is null
     */
    public ImageFileInputStream(InputStream fin, ByteOrder order)
    {
        if (fin == null)
        {
            throw new NullPointerException("InputStream cannot be null");
        }

        /* Note: NullPointerException may be thrown */
        this.byteOrder = Objects.requireNonNull(order, "Byte order cannot be null");
        this.stream = new DataInputStream(new BufferedInputStream(fin));
        this.streamPosition = 0L;
    }

    /**
     * Constructs a reader for the specified input file with a given byte order.
     *
     * @param fpath
     *        the path to the image file to be read
     * @param order
     *        the byte order to use when interpreting multi-byte values
     * 
     * @throws IOException
     *         if an I/O error occurs when opening the file
     * @throws NullPointerException
     *         if the file path or byte order is null
     */
    public ImageFileInputStream(Path fpath, ByteOrder order) throws IOException
    {
        this(Files.newInputStream(Objects.requireNonNull(fpath, "File path cannot be null")), order);
    }

    /**
     * Constructs a reader for the specified input stream with big-endian byte order.
     *
     * @param fin
     *        the input stream to wrap. Must not be null
     * 
     * @throws NullPointerException
     *         if {@code fin} is null
     * @see #ImageFileInputStream(InputStream, ByteOrder)
     */
    public ImageFileInputStream(InputStream fin)
    {
        this(fin, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Constructs a reader for the specified input file with big-endian byte order.
     *
     * @param fpath
     *        the path to the image file to be read
     * 
     * @throws IOException
     *         if an I/O error occurs when opening the file
     * @throws NullPointerException
     *         if the file path or byte order is null
     */
    public ImageFileInputStream(Path fpath) throws IOException
    {
        this(fpath, ByteOrder.BIG_ENDIAN);
    }

    /**
     * Sets the byte order used for interpreting multi-byte values.
     *
     * <p>
     * For example, reading the byte sequence {@code 0x01 0x02 0x03 0x04} as a 4-byte integer yields
     * {@code 0x01020304} in big-endian order, and {@code 0x04030201} in little-endian order.
     * </p>
     *
     * @param order
     *        the byte order to use, either {@code ByteOrder.BIG_ENDIAN} or
     *        {@code ByteOrder.LITTLE_ENDIAN}. It must not be null
     * 
     * @throws NullPointerException
     *         if order is null
     */
    public void setByteOrder(ByteOrder order)
    {
        if (order == null)
        {
            throw new NullPointerException("Byte order cannot be null");
        }

        byteOrder = order;
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
     * Skips {@code n} bytes in the stream, guaranteeing that the stream position is advanced by
     * exactly that amount, unless the end of the stream is reached.
     *
     * @param n
     *        the number of bytes to skip. Must be non-negative
     * @throws IOException
     *         if an I/O error occurs or the stream ends prematurely
     */
    public void skip(long n) throws IOException
    {
        if (n < 0)
        {
            throw new IllegalArgumentException("Number of bytes to skip cannot be negative");
        }

        long bytesSkipped = 0;
        byte[] buffer = new byte[1024];

        while (bytesSkipped < n)
        {
            int len = (int) Math.min(n - bytesSkipped, buffer.length);
            int result = stream.read(buffer, 0, len);

            if (result == -1) // EOF
            {
                throw new IOException("Premature end of stream encountered while skipping [" + n + "] bytes");
            }

            bytesSkipped += result;
        }

        streamPosition += n;
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

    // TEST IT

    /**
     * Seeks to a specific position in the stream.
     *
     * @param n
     *        the position to seek to
     * 
     * @throws IOException
     *         if an I/O error occurs, or if the stream does not support seeking
     */
    public void seek(long n) throws IOException
    {
        if (n < 0)
        {
            throw new IllegalArgumentException("Position cannot be negative");
        }

        if (n == streamPosition)
        {
            return;
        }

        else if (n > streamPosition)
        {
            skip(n - streamPosition);
        }
    }
}