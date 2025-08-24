package jpg;

/**
 * This enumeration represents the recognised segment identifiers often present in JPEG files.
 * 
 * Refer to the following resources for more information:
 * https://www.media.mit.edu/pia/Research/deepview/exif.html
 * https://download.osgeo.org/libtiff/doc/TIFF6.pdf
 * http://www.ozhiker.com/electronics/pjmt/jpeg_info/app_segments.html
 * http://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/JPEG.html
 * https://docs.fileformat.com/image/jpeg/
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public enum JpegSegmentConstantsOrig
{
    /* Marker for the start of the image data */
    START_OF_IMAGE(0xFF, 0xD8, "Start of Image"),

    /* Segment identifier for APP0, which usually includes JFIF or JFXX */
    APP0_SEGMENT(0xFF, 0xE0, "APP0 Segment for JFIF"),

    /* Segment identifier for APP1, typically containing Exif data */
    APP1_SEGMENT(0xFF, 0xE1, "APP1 Segment for EXIF"),

    /* Start Of Scan */
    START_OF_STREAM(0xFF, 0xDA, "Start Of Stream"),

    /* JPEG comment section */
    COMMENT(0xFF, 0xFE, "Comment Segment for JPG"),

    /* Marker for the end of the image data */
    END_OF_IMAGE(0xFF, 0xD9, "End of Image");

    private final byte marker;
    private final byte flag;
    private final String description;

    JpegSegmentConstantsOrig(int first, int second, String desc)
    {
        marker = (byte) (first & 0xFF);
        flag = (byte) (second & 0xFF);
        description = desc;
    }

    public byte getMarker()
    {
        return marker;
    }
    
    public byte getFlag()
    {
        return flag;
    }
    
    public String getDescription()
    {
        return description;
    }

    public static void displayAllMarkers()
    {
        for (JpegSegmentConstantsOrig segment : JpegSegmentConstantsOrig.values())
        {
            System.out.printf("%02X %02X\t%s%n", segment.marker, segment.flag, segment.description);
        }
    }
}