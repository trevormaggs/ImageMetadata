package png;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import logger.LogFactory;

/**
 * Represents the various types of chunks found in a PNG image file, as defined by the PNG
 * specification. This enum provides a centralised, type-safe way to interact with chunk identifiers
 * and their associated properties.
 *
 * <p>
 * Each enum constant corresponds to a specific PNG chunk type, for example: IHDR, IDAT or tEXt, and
 * stores its unique identifier (name), a descriptive text, a category for grouping similar chunks,
 * and a flag indicating whether multiple instances of this chunk type are allowed within a PNG
 * file.
 * </p>
 *
 * <p>
 * For detailed understanding of the PNG format, including chunk structure and meanings of various
 * chunks, refer to the official W3C PNG Specification:
 * <a href="https://www.w3.org/TR/png/#4Concepts">https://www.w3.org/TR/png/#4Concepts</a>.
 * </p>
 *
 * <p>
 * Change History:
 * </p>
 *
 * <ul>
 * <li>Version 1.0 - Initial release by Trevor Maggs on 28 July 2025</li>
 * </ul>
 *
 * @version 1.0
 * @author Trevor Maggs, trevmaggs@tpg.com.au
 * @since 28 July 2025
 */
public enum ChunkType
{
    IHDR(1, "IHDR", "Image header", Category.HEADER),
    PLTE(2, "PLTE", "Palette", Category.PALETTE),
    IDAT(3, "IDAT", "Image data", Category.DATA, true),
    IEND(4, "IEND", "Image trailer", Category.END),
    acTL(5, "acTL", "Animation Control Chunk", Category.ANIMATION),
    cHRM(6, "cHRM", "Primary chromaticities and white point", Category.COLOUR),
    cICP(7, "cICP", "Coding-independent code points for video signal type identification", Category.COLOUR),
    gAMA(8, "gAMA", "Image Gamma", Category.COLOUR),
    iCCP(9, "iCCP", "Embedded ICC profile", Category.COLOUR),
    mDCV(10, "mDCV", "Mastering Display Color Volume", Category.COLOUR),
    cLLI(11, "cLLI", "Content Light Level Information", Category.COLOUR),
    sBIT(12, "sBIT", "Significant bits", Category.COLOUR),
    sRGB(13, "sRGB", "Standard RGB color space", Category.COLOUR),
    bKGD(14, "bKGD", "Background color", Category.MISC),
    hIST(15, "hIST", "Image Histogram", Category.MISC),
    tRNS(16, "tRNS", "Transparency", Category.TRANSP),
    eXIf(17, "eXIf", "Exchangeable Image File Profile", Category.MISC),
    fcTL(18, "fcTL", "Frame Control Chunk", Category.ANIMATION, true),
    pHYs(19, "pHYs", "Physical pixel dimensions", Category.MISC),
    sPLT(20, "sPLT", "Suggested palette", Category.MISC, true),
    fdAT(21, "fdAT", "Frame Data Chunk", Category.ANIMATION, true),
    tIME(22, "tIME", "Image last-modification time", Category.TIME),
    iTXt(23, "iTXt", "International textual data", Category.TEXTUAL, true),
    tEXt(24, "tEXt", "Textual data", Category.TEXTUAL, true),
    zTXt(25, "zTXt", "Compressed textual data", Category.TEXTUAL, true),
    UNKNOWN(99, "Unknown", "Undefined to cover unknown chunks", Category.UNDEFINED);

    /**
     * Defines categories for PNG chunk types, grouping them by their general purpose. This helps in
     * organising and filtering chunks during processing.
     */
    public enum Category
    {
        HEADER("Image Header"),
        PALETTE("Palette table"),
        DATA("Image Data"),
        END("Image Trailer"),
        COLOUR("Colour Space"),
        MISC("Miscellaneous"),
        TRANSP("Transparency"),
        TEXTUAL("Textual"),
        ANIMATION("Animation"),
        TIME("Modified Time"),
        UNDEFINED("Undefined");

        private final String desc;

        /**
         * Constructs a Category enum with a descriptive name.
         * 
         * @param name
         *        the human-readable description of the category
         */
        private Category(String name)
        {
            desc = name;
        }

        /**
         * Returns the human-readable description of this chunk category.
         * 
         * @return The description of the category
         */
        public String getDescription()
        {
            return desc;
        }
    }

    private static final LogFactory LOGGER = LogFactory.getLogger(ChunkType.class);
    private final int index;
    private final String name;
    private final String description;
    private final Category category;
    private final boolean multipleAllowed;
    private final byte[] realChunk;

    /**
     * Constructs a ChunkType enum constant for chunks.
     *
     * @param index
     *        a unique integer identifier for the chunk type
     * @param name
     *        the 4-character ASCII name of the chunk type, i.e. "IHDR"
     * @param desc
     *        a human-readable description of the chunk's purpose
     * @param category
     *        the {@link Category} to which this chunk belongs
     */
    private ChunkType(int index, String name, String desc, Category category)
    {
        this(index, name, desc, category, false);
    }

    /**
     * Constructs a ChunkType enum constant, specifying whether multiple instances of this chunk are
     * allowed in a PNG file.
     *
     * @param index
     *        a unique integer identifier for the chunk type
     * @param name
     *        the 4-character ASCII name of the chunk type, i.e. "IHDR"
     * @param desc
     *        a human-readable description of the chunk's purpose
     * @param category
     *        the {@link Category} to which this chunk belongs
     * @param multipleAllowed
     *        {@code true} if multiple instances of this chunk type can appear in a PNG file,
     *        otherwise false
     */
    private ChunkType(int index, String name, String desc, Category category, boolean multipleAllowed)
    {
        this.index = index;
        this.name = name;
        this.description = desc;
        this.category = category;
        this.multipleAllowed = multipleAllowed;
        this.realChunk = Arrays.copyOf(name.getBytes(StandardCharsets.ISO_8859_1), name.length());
    }

    /**
     * Returns the unique integer identifier for this chunk type. This index is primarily for
     * internal enumeration and organising, not part of the PNG specification.
     * 
     * @return the integer index ID
     */
    public int getIndexID()
    {
        return index;
    }

    /**
     * Returns the 4-character ASCII name of this chunk type. This is the identifier used in the PNG
     * file format.
     * 
     * @return the chunk name, for example: "IHDR"
     */
    public String getChunkName()
    {
        return name;
    }

    /**
     * Returns a human-readable description of this chunk type's purpose.
     * 
     * @return the description of the chunk
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the category to which this chunk type belongs.
     * 
     * @return the {@link Category} of the chunk
     */
    public Category getCategory()
    {
        return category;
    }

    /**
     * Checks if multiple instances of this chunk type are allowed within a single PNG file. Some
     * chunks, for example: like IHDR and IEND, must appear only once, while others, such as IDAT,
     * tEXt, etc can appear multiple times.
     * 
     * @return true if multiple occurrences are allowed, otherwise false
     */
    public boolean isMultipleAllowed()
    {
        return multipleAllowed;
    }

    /**
     * Validates a 4-byte array to ensure it represents a valid PNG chunk type identifier. A valid
     * chunk type identifier must be exactly four bytes long and each byte must be an ASCII
     * alphabetic character (A-Z or a-z).
     *
     * @param bytes
     *        the 4-byte array representing a potential chunk type
     * 
     * @return false if the byte array is not 4 bytes long or contains non-alphabetic characters,
     *         otherwise true
     */
    public static boolean validateChunkBytes(byte[] bytes)
    {
        if (bytes.length != 4)
        {
            LOGGER.error("PNG chunk type identifier must be four bytes in length");
            return false;
        }

        for (byte b : bytes)
        {
            /* Letters must be [A-Z] or [a-z] */
            if (((b > 64 && b < 91) || (b > 96 && b < 123)) == false)
            {
                LOGGER.error("PNG chunk type identifier must only contain alphabet characters");
                return false;
            }
        }

        return true;
    }

    /**
     * Retrieves the {@code ChunkType} enum constant corresponding to the specified 4-byte chunk
     * identifier. The input bytes are validated to ensure completeness.
     *
     * @param chunk
     *        the 4-byte array representing the chunk type
     * 
     * @return the {@link ChunkType} enum constant if a match is found, or {@link #UNKNOWN} if the
     *         chunk type is not defined
     */
    public static ChunkType getChunkType(byte[] chunk)
    {
        if (validateChunkBytes(chunk))
        {
            for (ChunkType type : values())
            {
                if (Arrays.equals(type.realChunk, chunk))
                {
                    return type;
                }
            }
        }

        return UNKNOWN;
    }

    /**
     * Checks if a given 4-byte array corresponds to a known {@code ChunkType} defined in this enum.
     *
     * @param chunk
     *        the 4-byte array representing the chunk type
     * 
     * @return true if the chunk type is known, otherwise false
     */
    public static boolean contains(byte[] chunk)
    {
        return contains(getChunkType(chunk));
    }

    /**
     * Checks whether the specified {@code ChunkType} enum constant is defined within this enum.
     * This method essentially confirms if tyhe specified {@code ChunkType} object is one of the
     * valid constants declared in this enum.
     *
     * @param type
     *        the {@link ChunkType} enum constant to check
     * 
     * @return true if the type is found in this enum, otherwise false
     */
    public static boolean contains(ChunkType type)
    {
        for (ChunkType chunk : values())
        {
            if (chunk == type)
            {
                return true;
            }
        }

        return false;
    }
}