package png.strategy;

import java.util.List;
import png.ChunkDirectory;
import png.ChunkType;

/**
 * Strategy to find a directory by its category.
 */
public class CategoryLookupStrategy<T extends ChunkDirectory> implements DirectoryLookupStrategy<T, ChunkType.Category>
{
    @Override
    public T find(List<T> components, ChunkType.Category category)
    {
        for (T dir : components)
        {
            if (dir.getDirectoryCategory() == category)
            {
                return dir;
            }
        }
        
        return null;
    }
}