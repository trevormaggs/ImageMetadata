package tif.TagEntries;

import tif.DirectoryIdentifier;
import tif.TagHint;

public interface Taggable
{
    public int getNumberID();
    public TagHint getHint();
    public DirectoryIdentifier getDirectoryType();
}