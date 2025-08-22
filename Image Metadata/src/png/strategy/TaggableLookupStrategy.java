package png.strategy;

import java.util.List;
import png.ChunkDirectory;
import tif.TagEntries.Taggable;

/**
 * Strategy to find a directory containing a specific taggable object.
 */
public class TaggableLookupStrategy<T extends ChunkDirectory> implements DirectoryLookupStrategy<T, Taggable>
{
    @Override
    public T find(List<T> components, Taggable tag)
    {
        for (T dir : components)
        {
            if (dir.contains(tag))
            {
                return dir;
            }
        }
        return null;
    }
}