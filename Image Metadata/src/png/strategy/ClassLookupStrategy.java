package png.strategy;

import java.util.List;

/**
 * Strategy to find a directory by its class type.
 */
public class ClassLookupStrategy<T> implements DirectoryLookupStrategy<T, Class<?>>
{
    @Override
    public T find(List<T> components, Class<?> clazz)
    {
        for (T dir : components)
        {
            if (clazz.isInstance(dir))
            {
                return dir;
            }
        }
        
        return null;
    }
}