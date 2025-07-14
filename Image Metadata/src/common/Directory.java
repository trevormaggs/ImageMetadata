package common;

import tif.TagEntries.Taggable;

public interface Directory<T> extends BaseMetadata, Iterable<T>
{
    public boolean contains(Taggable tag);
    public T findEntryByID(int tagid);
    public int length();
    public boolean add(T entry);
    public boolean remove(T entry);
}