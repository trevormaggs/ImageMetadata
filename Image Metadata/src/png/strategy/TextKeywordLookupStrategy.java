package png.strategy;

import java.util.List;
import png.ChunkDirectory;
import png.TextKeyword;

/**
 * Strategy to find a directory containing a specific textual keyword.
 */
public class TextKeywordLookupStrategy<T extends ChunkDirectory> implements DirectoryLookupStrategy<T, TextKeyword>
{
    @Override
    public T find(List<T> components, TextKeyword keyword)
    {
        for (T dir : components)
        {
            if (dir.existsTextualKeyword(keyword))
            {
                return dir;
            }
        }

        return null;
    }
}