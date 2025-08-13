package tif.TagEntries;

import java.util.HashMap;
import java.util.Map;
import png.ChunkType;
import tif.DirectoryIdentifier;
import tif.TagHint;

/**
 * Enumerates PNG chunk types as tag representations that can be used in TIFF-style metadata
 * processing. This enum maps specific {@link ChunkType} values into taggable metadata elements,
 * enabling integration of PNG metadata into abstract directory structures, including
 * {@code DirectoryIFD}.
 *
 * <p>
 * These tags are primarily used in PNG metadata parsing systems where each chunk may be interpreted
 * as a metadata tag for consistency across image formats.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public enum TagPngChunk implements Taggable
{
    /** Image header data (first chunk) */
    CHUNK_TAG_IMAGE_HEADER(ChunkType.IHDR),

    /** Color palette definition */
    CHUNK_TAG_PALETTE(ChunkType.PLTE),

    /** Core image data (compressed pixels) */
    CHUNK_TAG_IMAGE_DATA(ChunkType.IDAT),

    /** Final trailer to mark PNG file end */
    CHUNK_TAG_IMAGE_TRAILER(ChunkType.IEND),

    /** Animation control data (APNG) */
    CHUNK_TAG_ANIMATION_CONTROL_CHUNK(ChunkType.acTL),

    /** Chromaticity coordinates */
    CHUNK_TAG_CHROMATICITIES(ChunkType.cHRM),

    /** Coding-independent color profile (cICP) */
    CHUNK_TAG_CODING_INDEPENDENT(ChunkType.cICP),

    /** Image gamma value */
    CHUNK_TAG_IMAGE_GAMMA(ChunkType.gAMA),

    /** Embedded ICC profile */
    CHUNK_TAG_EMBEDDED_ICC_PROFILE(ChunkType.iCCP),

    /** Mastering display metadata (HDR) */
    CHUNK_TAG_MASTERING_DISPLAY(ChunkType.mDCV),

    /** Content light level info (HDR) */
    CHUNK_TAG_CONTENT_LIGHT_LEVEL(ChunkType.cLLI),

    /** Significant bits info per channel */
    CHUNK_TAG_SIGNIFICANT_BITS(ChunkType.sBIT),

    /** Standard RGB color space */
    CHUNK_TAG_STANDARD_RBG(ChunkType.sRGB),

    /** Background color data */
    CHUNK_TAG_BACKGROUND_COLOUR(ChunkType.bKGD),

    /** Image histogram */
    CHUNK_TAG_IMAGE_HISTOGRAM(ChunkType.hIST),

    /** Transparency info */
    CHUNK_TAG_TRANSPARENCY(ChunkType.tRNS),

    /** Embedded Exif profile */
    CHUNK_TAG_EXIF_PROFILE(ChunkType.eXIf),

    /** Frame control chunk (APNG) */
    CHUNK_TAG_FRAME_CONTROL_CHUNK(ChunkType.fcTL),

    /** Physical pixel dimensions */
    CHUNK_TAG_PHYSICAL_PIXELS(ChunkType.pHYs),

    /** Suggested palette data */
    CHUNK_TAG_SUGGESTED_PALETTE(ChunkType.sPLT),

    /** Frame data chunk (APNG) */
    CHUNK_TAG_FRAME_DATA_CHUNK(ChunkType.fdAT),

    /** Last modification timestamp */
    CHUNK_TAG_LAST_MODIFICATION_TIME(ChunkType.tIME),

    /** UTF-8 textual metadata (international) */
    CHUNK_TAG_INTERNATIONAL_TEXT(ChunkType.iTXt),

    /** Basic Latin textual metadata */
    CHUNK_TAG_TEXTUAL_DATA(ChunkType.tEXt),

    /** Compressed textual metadata */
    CHUNK_TAG_COMPRESSED_TEXTUAL_DATA(ChunkType.zTXt);

    /** The PNG chunk type this tag represents. */
    private final ChunkType chunkType;

    /** A hint used for metadata interpretation. Default is {@link TagHint#HINT_DEFAULT}. */
    private final TagHint hint;

    /** Cached map for fast lookup of tags by chunk type. */
    private static final Map<ChunkType, TagPngChunk> CHUNK_LOOKUP = new HashMap<>();

    // Static initialiser to populate lookup map
    static
    {
        for (TagPngChunk tag : values())
        {
            CHUNK_LOOKUP.put(tag.chunkType, tag);
        }
    }

    /**
     * Constructs a new {@code TagPngChunk} instance wrapping the specified PNG chunk type.
     *
     * @param chunkType
     *        the chunk type to wrap
     */
    private TagPngChunk(ChunkType chunkType)
    {
        this.chunkType = chunkType;
        this.hint = TagHint.HINT_DEFAULT;
    }

    /**
     * Returns the associated {@link ChunkType} represented by this tag.
     *
     * @return the PNG chunk type
     */
    public ChunkType getChunkType()
    {
        return chunkType;
    }

    /**
     * Returns the numeric identifier associated with the PNG chunk type. This corresponds to the
     * 4-byte integer index of the chunk type.
     *
     * @return an integer tag ID
     */
    @Override
    public int getNumberID()
    {
        return chunkType.getIndexID();
    }

    /**
     * Returns the interpretation hint for this tag.
     *
     * @return a {@link TagHint}. For the time being, it is always {@link TagHint#HINT_DEFAULT}
     */
    @Override
    public TagHint getHint()
    {
        return hint;
    }

    /**
     * Returns the metadata directory this tag is mapped to. For PNG, this defaults to
     * {@link DirectoryIdentifier#TIFF_DIRECTORY_UNKNOWN}.
     *
     * @return the directory identifier
     */
    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return DirectoryIdentifier.TIFF_DIRECTORY_UNKNOWN;
    }

    /**
     * Looks up the {@code TagPngChunk} instance for the specified {@link ChunkType}.
     *
     * @param type
     *        the PNG chunk type to look up
     * @return the corresponding TagPngChunk, or null if not found
     */
    public static TagPngChunk getTagType(ChunkType type)
    {
        return CHUNK_LOOKUP.get(type);
    }

    /**
     * Returns a string representation of the tag, including the enum name and chunk type.
     *
     * @return a formatted string representation
     */
    @Override
    public String toString()
    {
        return String.format("%s (%s)", name(), chunkType);
    }
}