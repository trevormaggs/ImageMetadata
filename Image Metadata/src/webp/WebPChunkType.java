package webp;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import common.ByteValueConverter;

/**
 * Represents the known chunk types found in a WebP file.
 *
 * <p>
 * WebP files are based on the Resource Interchange File Format (RIFF) and store data in a series of
 * chunks, each identified by a four-character code (FourCC).
 * </p>
 *
 * <p>
 * This enum provides a descriptive representation of these chunk types, along with utility methods
 * to find a type from its FourCC string or integer value.
 * </p>
 *
 * <p>
 * This class correctly handles the little-endian byte ordering used for FourCCs in the RIFF
 * specification.
 * </p>
 */
public enum WebPChunkType
{
    /** The main container chunk for the entire WebP file. */
    RIFF("RIFF", "Header"),

    /** The WebP image descriptor, indicating the file is a WebP image. */
    WEBP("WEBP", "Image Descriptor"),

    /** Contains lossy-compressed image data (note the trailing space). */
    VP8("VP8 ", "Lossy WebP Format"),

    /** Contains lossless-compressed image data. */
    VP8L("VP8L", "Lossless WebP Format"),

    /** Indicates an extended WebP file format with optional features. */
    VP8X("VP8X", "Extended File Format"),

    /** Contains the embedded colour profile (e.g., ICC profile). */
    ICCP("ICCP", "Colour Profile"),

    /** Global parameters for an animated WebP file. */
    ANIM("ANIM", "Global Parameters for Animation"),

    /** Contains Exif metadata. */
    EXIF("EXIF", "Exif Metadata"),

    /** Contains XMP (Extensible Metadata Platform) metadata (note the trailing space). */
    XMP("XMP ", "XMP Metadata"),

    /** Contains the alpha (transparency) channel data. */
    ALPH("ALPH", "Alpha Channel"),

    /** Contains frame information for an animated WebP file. */
    ANMF("ANMF", "Frame information for Animation"),

    /** Represents an unrecognised chunk type. */
    OTHER("WXYZ", "Other");

    private final String chunkType;
    private final String description;
    private final int fourccValue;
    private static final Map<Integer, WebPChunkType> LOOKUP_MAP = new HashMap<>();

    static
    {
        for (WebPChunkType type : values())
        {
            LOOKUP_MAP.put(type.getFourccValue(), type);
        }
    }

    /**
     * Constructs a new WebPChunkType enum constant.
     *
     * @param fourCC
     *        a four-character code string, for example: {@code RIFF}, {@code VP8 } etc
     * @param desc
     *        a human-readable description of the chunk type
     */
    private WebPChunkType(String fourCC, String desc)
    {
        this.chunkType = fourCC;
        this.description = desc;
        this.fourccValue = ByteValueConverter.toInteger(fourCC.getBytes(StandardCharsets.US_ASCII), ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Gets the four-character code (FourCC) for this chunk type.
     *
     * @return the 4-character chunk name string
     */
    public String getChunkName()
    {
        return chunkType;
    }

    /**
     * Gets a human-readable description of the chunk type.
     *
     * @return the description of the chunk
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Gets the 32-bit integer representation of the FourCC (four-character code), using
     * little-endian byte order as used in RIFF.
     *
     * @return the 32-bit integer value of the FourCC
     */
    public int getFourccValue()
    {
        return fourccValue;
    }

    /**
     * Finds a {@code WebPChunkType} enum constant from its integer FourCC value.
     *
     * <p>
     * This method performs a fast lookup using a cached map.
     * </p>
     *
     * @param fourcc
     *        the 32-bit integer value of the FourCC
     *
     * @return The matching WebPChunkType or OTHER if no match is found
     */
    public static WebPChunkType findType(int fourcc)
    {
        return LOOKUP_MAP.getOrDefault(fourcc, OTHER);
    }

    /**
     * Finds a {@code WebPChunkType} enum constant from its FourCC string.
     *
     * @param chunkType
     *        the four-character code string
     *
     * @return The matching WebPChunkType or OTHER if no match is found
     */
    public static WebPChunkType findType(String chunkType)
    {
        for (WebPChunkType type : values())
        {
            if (Objects.equals(type.chunkType, chunkType))
            {
                return type;
            }
        }

        return OTHER;
    }

    /**
     * Converts a 32-bit integer FourCC value into its corresponding 4-character string
     * representation, following little-endian byte order.
     *
     * @param value
     *        the integer FourCC value
     *
     * @return the 4-character code string
     */
    public static String getChunkName(int value)
    {
        byte[] b = new byte[4];

        // This follows Little Endian-ness format
        b[0] = (byte) (value & 0x000000ff);
        b[1] = (byte) ((value & 0x0000ff00) >> 8);
        b[2] = (byte) ((value & 0x00ff0000) >> 16);
        b[3] = (byte) ((value & 0xff000000) >>> 24);

        return new String(b, StandardCharsets.US_ASCII);
    }
}