package png;

import java.util.Arrays;

/**
 * Basic functionalities are provided to interact with chunk fields, particularly within PNG image
 * files.
 * 
 * For detailed understanding of the PNG format, refer to
 * https://www.w3.org/TR/png/#4Concepts.
 * 
 * <p>
 * Change History:
 * </p>
 * 
 * <ul>
 * <li>Version 1.0 - Initial release by Trevor Maggs on 21 June 2025</li>
 * </ul>
 * 
 * @version 0.1
 * @author Trevor Maggs, trevmaggs@tpg.com.au
 * @since 21 June 2025
 */
public enum ChunkType
{
    IHDR(1, "IHDR", "Image header", Category.HEADER),
    PLTE(2, "PLTE", "Palette", Category.PALETTE),
    IDAT(3, "IDAT", "Image data", Category.DATA, true),
    IEND(4, "IEND", "Image trailer", Category.END),
    acTL(5, "acTL", "Animation Control Chunk", Category.ANIMINATION),
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
    fcTL(18, "fcTL", "Frame Control Chunk", Category.ANIMINATION, true),
    pHYs(19, "pHYs", "Physical pixel dimensions", Category.MISC),
    sPLT(20, "sPLT", "Suggested palette", Category.MISC, true),
    fdAT(21, "fdAT", "Frame Data Chunk", Category.ANIMINATION, true),
    tIME(22, "tIME", "Image last-modification time", Category.TIME),
    iTXt(23, "iTXt", "International textual data", Category.TEXTUAL, true),
    tEXt(24, "tEXt", "Textual data", Category.TEXTUAL, true),
    zTXt(25, "zTXt", "Compressed textual data", Category.TEXTUAL, true),
    UNKNOWN(99, "Unknown", "Undefined to cover unknown chunks", Category.UNDEFINED);

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
        ANIMINATION("Animination"),
        TIME("Modified Time"),
        UNDEFINED("Undefined");

        private final String desc;

        private Category(String name)
        {
            desc = name;
        }

        public String getDescription()
        {
            return desc;
        }
    }

    private final int index;
    private final String name;
    private final String description;
    private final Category category;
    private final boolean multipleAllowed;
    private final byte[] realChunk;

    private ChunkType(int index, String name, String desc, Category category)
    {
        this(index, name, desc, category, false);
    }

    private ChunkType(int index, String name, String desc, Category category, boolean multipleAllowed)
    {
        this.index = index;
        this.name = name;
        this.description = desc;
        this.category = category;
        this.multipleAllowed = multipleAllowed;
        this.realChunk = Arrays.copyOf(name.getBytes(), name.length());
    }

    public int getIndexID()
    {
        return index;
    }

    public String getChunkName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public Category getCategory()
    {
        return category;
    }

    public boolean isMultipleAllowed()
    {
        return multipleAllowed;
    }

    public static void validateChunkBytes(byte[] bytes)
    {
        if (bytes.length != 4)
        {
            throw new IllegalArgumentException("PNG chunk type identifier must be four bytes in length");
        }

        for (byte b : bytes)
        {
            /* Letters must be [A-Z] or [a-z] */
            if ((b < 65 && b > 90) && (b < 97 && b > 122))
            {
                throw new IllegalArgumentException("PNG chunk type identifier must only contain alphabet characters");
            }
        }
    }

    public static ChunkType getChunkType(byte[] chunk)
    {
        validateChunkBytes(chunk);

        for (ChunkType type : values())
        {
            if (Arrays.equals(type.realChunk, chunk))
            {
                return type;
            }
        }

        return UNKNOWN;
    }

    public static boolean contains(byte[] chunk)
    {
        return contains(getChunkType(chunk));
    }

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