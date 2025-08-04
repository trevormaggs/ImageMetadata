package png;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.CRC32;
import common.ByteValueConverter;
import tif.TagEntries.TagPngChunk;

/**
 * Represents an individual chunk in a PNG file.
 *
 * <p>
 * Each chunk contains raw byte data, a type identifier, and optionally Exif data. This class also
 * supports flag decoding and keyword/value extraction for textual chunks.
 * </p>
 *
 * <p>
 * Refer to the PNG Specification for information on chunk layout and bit-flag meanings.
 * </p>
 *
 * @version 0.1
 * @since 21 June 2025
 */
public class PngChunk
{
    private final long length;
    private final byte[] typeBytes;
    private final int crc;
    protected final byte[] payload;
    private final boolean ancillaryBit;
    private final boolean privateBit;
    private final boolean reservedBit;
    private final boolean safeToCopyBit;

    /**
     * Constructs a new {@code PngChunk}, including an optional Exif parser.
     *
     * @param length
     *        the length of the chunk's data field (excluding type and CRC)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     */
    public PngChunk(long length, byte[] typeBytes, int crc32, byte[] data)
    {
        this.length = length;
        this.typeBytes = Arrays.copyOf(typeBytes, typeBytes.length);
        this.crc = crc32;
        this.payload = Arrays.copyOf(data, data.length);

        boolean[] flags = extractPropertyBits(ByteValueConverter.toInteger(typeBytes, ByteOrder.BIG_ENDIAN));
        this.ancillaryBit = flags[0];
        this.privateBit = flags[1];
        this.reservedBit = flags[2];
        this.safeToCopyBit = flags[3];
    }

    /**
     * Extracts the 5th-bit flags from each byte of the chunk type name. Used to determine
     * ancillary/private/reserved/safe-to-copy properties.
     *
     * In a nutshell, it examines Bit 5 to determine whether the corresponding bit is upper-case or
     * lower-case. If Bit 5 is 0, it indicates an upper-case letter. If this bit is a one, it is
     * lower-case.
     *
     * @param value
     *        the integer representation of the 4-byte chunk type
     *
     * @return boolean array of flags, including [ancillary, private, reserved and safeToCopy bits
     */
    private static boolean[] extractPropertyBits(int value)
    {
        boolean[] flags = new boolean[4];
        int shift = 24;
        int mask = 1 << 5;

        for (int i = 0; i < flags.length; i++)
        {
            int b = (value >> shift) & 0xFF;

            flags[i] = (b & mask) != 0;
            shift -= 8;
        }

        return flags;
    }

    /**
     * Retrieves the length of data bytes held by this chunk.
     *
     * @return the length
     */
    public long getLength()
    {
        return length;
    }

    /**
     * Returns the chunk type.
     *
     * @return the type defined as s {@link ChunkType}
     */
    public ChunkType getType()
    {
        return ChunkType.getChunkType(typeBytes);
    }

    /**
     * Retrieves the corresponding tag used in PNG metadata processing.
     *
     * @return the {@link TagPngChunk} representation of the chunk type
     */
    public TagPngChunk getTag()
    {
        return TagPngChunk.getTagType(getType());
    }

    /**
     * Returns a four-byte CRC computed on the preceding bytes in the chunk, excluding the
     * length field.
     *
     * @return a CRC value defined as an integer
     */
    public int getCrc()
    {
        return crc;
    }

    /**
     * Calculates the CRC-32 checksum for this chunk (type code + data).
     *
     * @return The calculated CRC-32 value.
     */
    public int calculateCrc()
    {
        CRC32 crc32 = new CRC32();

        crc32.update(typeBytes);
        crc32.update(payload);

        return (int) crc32.getValue();
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
     * This method should be sub-classed by one of the textual chunks to create useful
     * functionality.
     *
     * @param keyword
     *        the {@link TextKeyword} to search for
     *
     * @return always false by default
     */
    public boolean hasKeywordPair(TextKeyword keyword)
    {
        return false;
    }

    /**
     * This method should be sub-classed by one of the textual chunks to create useful
     * functionality.
     *
     * @return always {@link Optional#empty()} by default
     */
    public Optional<TextEntry> getKeywordPair()
    {
        return Optional.empty();
    }

    /**
     * Validates the chunk is ancillary.
     *
     * @return true if the chunk is ancillary, otherwise, it is false
     */
    public boolean isAncillary()
    {
        return ancillaryBit;
    }

    /**
     * Validates the chunk is private.
     *
     * @return true if the chunk is private, otherwise, it is false
     */
    public boolean isPrivate()
    {
        return privateBit;
    }

    /**
     * Validates the chunk is reserved.
     *
     * @return true if the chunk is reserved, otherwise, it is false
     */
    public boolean isReserved()
    {
        return reservedBit;
    }

    /**
     * Validates the chunk is safe to copy.
     *
     * @return true to indicate the chunk is safe to copy
     */
    public boolean isSafeToCopy()
    {
        return safeToCopyBit;
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

        if (!(obj instanceof PngChunk))
        {
            return false;
        }

        PngChunk other = (PngChunk) obj;

        return (length == other.length &&
                Arrays.equals(typeBytes, other.typeBytes) &&
                crc == other.crc &&
                Arrays.equals(payload, other.payload));
    }

    /**
     * Computes a hash code consistent with {@link #equals}.
     *
     * @return hash code for this chunk
     */
    @Override
    public int hashCode()
    {
        int result = Objects.hash(length, crc);

        result = 31 * result + Arrays.hashCode(typeBytes);
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
        String[] parts = ByteValueConverter.splitNullDelimitedStrings(getPayloadArray());

        line.append(String.format(" %-20s %s%n", "[Tag Name]", getTag()));
        line.append(String.format(" %-20s %s%n", "[Data Length]", getLength()));
        line.append(String.format(" %-20s %s%n", "[Chunk Type]", getType()));
        line.append(String.format(" %-20s %s%n", "[CRC Value ]", getCrc()));
        line.append(String.format(" %-20s %s%n", "[Byte Values]", Arrays.toString(payload)));
        line.append(String.format(" %-20s %s%n", "[Textual]", Arrays.toString(parts)));

        return line.toString();
    }
}