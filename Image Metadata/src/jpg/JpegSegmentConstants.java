package jpg;

import java.util.HashMap;
import java.util.Map;

/**
 * This enumeration represents the known segment identifiers, that are often present in JPEG files.
 * 
 * <p>
 * JPEG files are structured as a series of segments, each beginning with a marker (0xFF) followed
 * by a flag byte. Some segments, especially APPn segments, can contain metadata such as EXIF, XMP,
 * ICC profiles, or Photoshop-specific data.
 * </p>
 * 
 * <p>
 * The enum also provides helper methods to identify segment types, determine APP numbers, and
 * quickly check if a segment may contain metadata.
 * </p>
 * 
 * @author Trevor
 * @version 1.1
 * @since 25 August 2025
 */
public enum JpegSegmentConstants
{
    START_OF_IMAGE(0xFF, 0xD8, "Start of Image", false),
    START_OF_STREAM(0xFF, 0xDA, "Start Of Scan", true), // SOS has a length field for its header
    END_OF_IMAGE(0xFF, 0xD9, "End of Image", false),
    COMMENT(0xFF, 0xFE, "Comment Segment for JPG", true),
    UNKNOWN(0xFF, 0x00, "Fallback: Unknown or unsupported marker", false),

    APP0_SEGMENT(0xFF, 0xE0, "APP0 Segment for JFIF", true),
    APP1_SEGMENT(0xFF, 0xE1, "APP1 Segment for EXIF", true),
    APP2_SEGMENT(0xFF, 0xE2, "APP2 Segment (ICC)", true),
    APP3_SEGMENT(0xFF, 0xE3, "APP3 Segment", true),
    APP4_SEGMENT(0xFF, 0xE4, "APP4 Segment", true),
    APP5_SEGMENT(0xFF, 0xE5, "APP5 Segment", true),
    APP6_SEGMENT(0xFF, 0xE6, "APP6 Segment", true),
    APP7_SEGMENT(0xFF, 0xE7, "APP7 Segment", true),
    APP8_SEGMENT(0xFF, 0xE8, "APP8 Segment", true),
    APP9_SEGMENT(0xFF, 0xE9, "APP9 Segment", true),
    APP10_SEGMENT(0xFF, 0xEA, "APP10 Segment", true),
    APP11_SEGMENT(0xFF, 0xEB, "APP11 Segment", true),
    APP12_SEGMENT(0xFF, 0xEC, "APP12 Segment", true),
    APP13_SEGMENT(0xFF, 0xED, "APP13 Segment (Photoshop)", true),
    APP14_SEGMENT(0xFF, 0xEE, "APP14 Segment", true),
    APP15_SEGMENT(0xFF, 0xEF, "APP15 Segment", true),

    // Restart markers (RST0–RST7) – no length field
    RST0(0xFF, 0xD0, "Restart Marker 0", false),
    RST1(0xFF, 0xD1, "Restart Marker 1", false),
    RST2(0xFF, 0xD2, "Restart Marker 2", false),
    RST3(0xFF, 0xD3, "Restart Marker 3", false),
    RST4(0xFF, 0xD4, "Restart Marker 4", false),
    RST5(0xFF, 0xD5, "Restart Marker 5", false),
    RST6(0xFF, 0xD6, "Restart Marker 6", false),
    RST7(0xFF, 0xD7, "Restart Marker 7", false),

    // TEM marker – no length field
    TEM(0xFF, 0x01, "Temporary Private Use Marker", false);

    private static final Map<Integer, JpegSegmentConstants> LOOKUP = new HashMap<>();
    private final int marker;
    private final int flag;
    private final String description;
    private final boolean hasLength;

    static
    {
        for (JpegSegmentConstants seg : values())
        {
            int key = ((seg.marker & 0xFF) << 8) | (seg.flag & 0xFF);
            LOOKUP.put(key, seg);
        }
    }

    private JpegSegmentConstants(int marker, int flag, String description, boolean hasLength)
    {
        this.marker = (marker & 0xFF);
        this.flag = (flag & 0xFF);
        this.description = description;
        this.hasLength = hasLength;
    }

    /**
     * Gets the marker byte for this JPG segment.
     * 
     * @return a byte value of the segment marker
     */
    public int getMarker()
    {
        return marker;
    }

    /**
     * Gets the flag byte for this JPG segment.
     * 
     * @return a byte value of the segment flag
     */
    public int getFlag()
    {
        return flag;
    }

    /**
     * Gets the description of the segment that is assigned to this enum.
     * 
     * @return the description in string
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns true if this segment has a 2-byte length field following the marker.
     * 
     * @return true if the segment is expected to have a length of data, otherwise false
     */
    public boolean hasLengthField()
    {
        return hasLength;
    }

    /**
     * Retrieves a segment constant from its marker and flag bytes.
     *
     * @param marker
     *        the first byte (usually 0xFF)
     * @param flag
     *        the second byte identifying the segment
     * @return the matching JpegSegmentConstants, or {@link #UNKNOWN} if not known
     */
    public static JpegSegmentConstants fromBytes(int marker, int flag)
    {
        int key = ((marker & 0xFF) << 8) | (flag & 0xFF);

        return LOOKUP.getOrDefault(key, UNKNOWN);
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
        return this != UNKNOWN && !isAppSegment();
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
     * 
     * @return true if the segment is an APP segment that may contain metadata
     */
    public static boolean isMetadataSegment(int marker, int flag)
    {
        return fromBytes(marker, flag).canContainMetadata();
    }

    /**
     * Displays all defined JPEG markers with their description.
     */
    public static void displayAllMarkers()
    {
        for (JpegSegmentConstants segment : values())
        {
            String extra = segment.isAppSegment() ? String.format(" (APP%d)", segment.getAppNumber()) : "";

            System.out.printf("%02X %02X\t%s%s [hasLengthField=%s]%n", segment.marker, segment.flag, segment.description, extra, segment.hasLength);
        }
    }
}