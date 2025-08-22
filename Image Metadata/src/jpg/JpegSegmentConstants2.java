package jpg;

import java.util.HashMap;
import java.util.Map;

/**
 * This enumeration represents the recognised segment identifiers often present in JPEG files.
 * 
 * <p>
 * JPEG files are structured as a series of segments, each beginning with a marker (0xFF) followed
 * by a flag byte. Some segments, especially APPn segments, can contain metadata such as EXIF, XMP,
 * ICC profiles, or Photoshop-specific data.
 * </p>
 * 
 * <p>
 * The enum provides helper methods to identify segment types, determine APP numbers, and quickly
 * check if a segment may contain metadata.
 * </p>
 * 
 * @author Trevor
 * @version 1.1
 * @since 22 August 2025
 */
public enum JpegSegmentConstants2
{
    START_OF_IMAGE(0xFF, 0xD8, "Start of Image"),
    START_OF_STREAM(0xFF, 0xDA, "Start Of Scan"),
    END_OF_IMAGE(0xFF, 0xD9, "End of Image"),
    COMMENT(0xFF, 0xFE, "Comment Segment for JPG"),

    APP0_SEGMENT(0xFF, 0xE0, "APP0 Segment for JFIF"),
    APP1_SEGMENT(0xFF, 0xE1, "APP1 Segment for EXIF"),
    APP2_SEGMENT(0xFF, 0xE2, "APP2 Segment (ICC)"),
    APP3_SEGMENT(0xFF, 0xE3, "APP3 Segment"),
    APP4_SEGMENT(0xFF, 0xE4, "APP4 Segment"),
    APP5_SEGMENT(0xFF, 0xE5, "APP5 Segment"),
    APP6_SEGMENT(0xFF, 0xE6, "APP6 Segment"),
    APP7_SEGMENT(0xFF, 0xE7, "APP7 Segment"),
    APP8_SEGMENT(0xFF, 0xE8, "APP8 Segment"),
    APP9_SEGMENT(0xFF, 0xE9, "APP9 Segment"),
    APP10_SEGMENT(0xFF, 0xEA, "APP10 Segment"),
    APP11_SEGMENT(0xFF, 0xEB, "APP11 Segment"),
    APP12_SEGMENT(0xFF, 0xEC, "APP12 Segment"),
    APP13_SEGMENT(0xFF, 0xED, "APP13 Segment (Photoshop)"),
    APP14_SEGMENT(0xFF, 0xEE, "APP14 Segment"),
    APP15_SEGMENT(0xFF, 0xEF, "APP15 Segment");

    public final byte marker;
    public final byte flag;
    public final String description;

    private static final Map<Integer, JpegSegmentConstants2> LOOKUP = new HashMap<>();

    static
    {
        for (JpegSegmentConstants2 seg : values())
        {
            int key = ((seg.marker & 0xFF) << 8) | (seg.flag & 0xFF);
            LOOKUP.put(key, seg);
        }
    }

    JpegSegmentConstants2(int marker, int flag, String description)
    {
        this.marker = (byte) (marker & 0xFF);
        this.flag = (byte) (flag & 0xFF);
        this.description = description;
    }

    /**
     * Retrieves a segment constant from its marker and flag bytes.
     * 
     * @param marker
     *        the first byte (0xFF)
     * @param flag
     *        the second byte identifying the segment
     * @return the corresponding {@code JpegSegmentConstants2 }, or {@code null} if unknown
     */
    public static JpegSegmentConstants2 fromBytes(byte marker, byte flag)
    {
        int key = ((marker & 0xFF) << 8) | (flag & 0xFF);

        return LOOKUP.get(key);
    }

    /**
     * Returns true if this segment is an APPn segment (0xFFE0–0xFFEF).
     * 
     * @return true if an APP segment
     */
    public boolean isAppSegment()
    {
        int unsignedFlag = flag & 0xFF;

        return unsignedFlag >= 0xE0 && unsignedFlag <= 0xEF;
    }

    /**
     * Returns the APP segment number (0–15), or -1 if not an APP segment.
     * 
     * @return APP number, or -1
     */
    public int getAppNumber()
    {
        if (!isAppSegment())
        {
            return -1;
        }

        return (flag & 0xFF) - 0xE0;
    }

    /**
     * Returns true if this segment is a standard marker (non-APP).
     * 
     * @return true if standard marker
     */
    public boolean isStandardMarker()
    {
        return !isAppSegment();
    }

    /**
     * Indicates whether this segment may contain metadata, such as EXIF, ICC profile, or Photoshop
     * info.
     * 
     * @return true if likely to contain metadata
     */
    public boolean canContainMetadata()
    {
        if (!isAppSegment())
        {
            return false;
        }

        switch (getAppNumber())
        {
            case 1: // APP1 - EXIF/XMP
            case 2: // APP2 - ICC profile
            case 13: // APP13 - Photoshop
                return true;
            default:
                return false;
        }
    }

    /**
     * Checks whether a given byte pair from a JPEG stream represents a metadata-carrying segment.
     * 
     * @param marker
     *        the first byte (should be 0xFF)
     * @param flag
     *        the second byte identifying the segment
     * @return true if the segment is an APP segment that may contain metadata
     */
    public static boolean isMetadataSegment(byte marker, byte flag)
    {
        JpegSegmentConstants2 seg = fromBytes(marker, flag);

        return seg != null && seg.canContainMetadata();
    }

    /**
     * Displays all defined JPEG markers with their description.
     */
    public static void displayAllMarkers()
    {
        for (JpegSegmentConstants2 segment : values())
        {
            System.out.printf("%02X %02X\t%s%n", segment.marker, segment.flag, segment.description);
        }
    }
}
