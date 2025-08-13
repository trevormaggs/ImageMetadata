package webp;

import java.util.Arrays;
import java.util.Objects;
import logger.LogFactory;

/**
 * Represents an individual chunk in a WebP file.
 *
 * <p>
 * Each chunk contains raw byte data, a type identifier, called FourCC and its size. This class also
 * provides rudimentary utility methods to support this class.
 * </p>
 *
 * <p>
 * Refer to the WebP specification for information on
 * <a href="https://developers.google.com/speed/webp/docs/riff_container">Google Developer site</a>
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class WebpChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(WebpChunk.class);
    private final int fourcc;
    private final int length;
    protected final byte[] payload;

    /**
     * Constructs a new {@code WebpChunk} instance to represent a single chunk.
     *
     * @param type
     *        the 32-bit FourCC chunk identifier (in little-endian integer form)
     * @param length
     *        the length of the chunk's payload
     * @param data
     *        raw chunk data
     */
    public WebpChunk(int type, int length, byte[] data)
    {
        this.fourcc = type;
        this.length = length;
        this.payload = Arrays.copyOf(data, data.length);

        if (WebPChunkType.findType(type) == WebPChunkType.OTHER)
        {
            LOGGER.warn("Unknown FourCC type [" + WebPChunkType.getChunkName(type) + "]");
        }
    }

    /**
     * Gets the 32-bit little-endian integer representation of the FourCC (four-character code).
     *
     * @return the 32-bit FourCC chunk identifier
     */
    public int getTypeValue()
    {
        return fourcc;
    }

    /**
     * Retrieves the length of data bytes held by this chunk.
     *
     * @return the length
     */
    public int getLength()
    {
        return length;
    }

    /**
     * Returns a defensive copy of the raw chunk data.
     *
     * @return the raw data as a byte sub-array
     */
    public byte[] getPayloadArray()
    {
        return Arrays.copyOf(payload, payload.length);
    }

    /**
     * Retrieves the {@code WebPChunkType} enum constant corresponding to the actual FourCC value.
     * 
     * @return the WebPChunkType constant
     */
    public WebPChunkType getType()
    {
        return WebPChunkType.findType(fourcc);
    }

    /**
     * Checks whether the chunk is known and valid.
     * 
     * @return true if the chunk type is known, otherwise false
     */
    public boolean isKnownType()
    {
        return getType() != WebPChunkType.OTHER;
    }

    /**
     * Compares this chunk with another for full equality.
     *
     * @param obj
     *        the object to compare
     *
     * @return true if equal in all fields
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof WebpChunk))
        {
            return false;
        }

        WebpChunk other = (WebpChunk) obj;

        return (fourcc == other.fourcc && length == other.length && Arrays.equals(payload, other.payload));
    }

    /**
     * Computes a hash code consistent with {@link #equals}.
     *
     * @return hash code for this chunk
     */
    @Override
    public int hashCode()
    {
        int result = Objects.hash(fourcc, length);

        result = 31 * result + Arrays.hashCode(payload);

        return result;
    }

    /**
     * Returns a string representation of the chunk's properties and contents.
     *
     * @return a formatted string describing this chunk
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(String.format(" %-20s %s%n", "[FourCC Type]", getType()));
        line.append(String.format(" %-20s %s%n", "[Type Value]", getTypeValue()));
        line.append(String.format(" %-20s %s%n", "[Payload Size]", getLength()));
        line.append(String.format(" %-20s %s%n", "[Byte Values]", Arrays.toString(payload)));

        return line.toString();
    }
}