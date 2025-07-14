package tif.TagEntries;

import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagGPS implements Taggable
{
    GPS_TAG_GPS_VERSION_ID(0x0000, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_LATITUDE_REF(0x0001, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_LATITUDE(0x0002, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_LONGITUDE_REF(0x0003, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_LONGITUDE(0x0004, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_ALTITUDE_REF(0x0005, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_ALTITUDE(0x0006, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_TIME_STAMP(0x0007, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_SATELLITES(0x0008, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_STATUS(0x0009, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_MEASURE_MODE(0x000A, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DOP(0x000B, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_SPEED_REF(0x000C, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_SPEED(0x000D, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_TRACK_REF(0x000E, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_TRACK(0x000F, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_IMG_DIRECTION_REF(0x0010, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_IMG_DIRECTION(0x0011, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_MAP_DATUM(0x0012, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DEST_LATITUDE_REF(0x0013, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DEST_LATITUDE(0x0014, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DEST_LONGITUDE_REF(0x0015, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DEST_LONGITUDE(0x0016, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DEST_BEARING_REF(0x0017, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DEST_BEARING(0x0018, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DEST_DISTANCE_REF(0x0019, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DEST_DISTANCE(0x001A, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_PROCESSING_METHOD(0x001B, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_AREA_INFORMATION(0x001C, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DATE_STAMP(0x001D, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_DIFFERENTIAL(0x001E, DirectoryIdentifier.EXIF_DIRECTORY_GPS),
    GPS_TAG_GPS_HPOSITIONING_ERROR(0x001F, DirectoryIdentifier.EXIF_DIRECTORY_GPS);

    private final int numID;
    private final DirectoryIdentifier directory;
    private final TagHint hint;

    private TagGPS(int id, DirectoryIdentifier dir)
    {
        this(id, dir, TagHint.HINT_DEFAULT);
    }

    private TagGPS(int id, DirectoryIdentifier dir, TagHint clue)
    {
        numID = id;
        directory = dir;
        hint = clue;
    }

    @Override
    public int getNumberID()
    {
        return numID;
    }

    @Override
    public DirectoryIdentifier getDirectoryType()
    {
        return directory;
    }

    @Override
    public TagHint getHint()
    {
        return hint;
    }
}