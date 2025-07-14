package tif.TagEntries;

import tif.DirectoryIdentifier;
import tif.TagHint;

public enum TagINTEROP implements Taggable
{
    INTEROP_TAG_INTEROP_INDEX(0x0001, DirectoryIdentifier.EXIF_DIRECTORY_INTEROP),
    INTEROP_TAG_INTEROP_VERSION(0x0002, DirectoryIdentifier.EXIF_DIRECTORY_INTEROP),
    INTEROP_TAG_RELATED_IMAGE_FILE_FORMAT(0x1000, DirectoryIdentifier.EXIF_DIRECTORY_INTEROP),
    INTEROP_TAG_RELATED_IMAGE_WIDTH(0x1001, DirectoryIdentifier.EXIF_DIRECTORY_INTEROP),
    INTEROP_TAG_RELATED_IMAGE_HEIGHT(0x1002, DirectoryIdentifier.EXIF_DIRECTORY_INTEROP);

    private final int numID;
    private final DirectoryIdentifier directory;
    private final TagHint hint;

    private TagINTEROP(int id, DirectoryIdentifier dir)
    {
        this(id, dir, TagHint.HINT_DEFAULT);
    }

    private TagINTEROP(int id, DirectoryIdentifier dir, TagHint clue)
    {
        numID = id;
        directory = dir;
        hint = clue;
    }

    public int getNumberID()
    {
        return numID;
    }

    public DirectoryIdentifier getDirectoryType()
    {
        return directory;
    }

    public TagHint getHint()
    {
        return hint;
    }
}