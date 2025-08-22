package png.strategy;

import java.util.List;

/**
 * Interface for a directory lookup strategy, abstracting different search methods.
 *
 * @param <T>
 *        The type of the metadata directory to be found
 * @param <U>
 *        The type of the lookup key or element
 */
public interface DirectoryLookupStrategy<T, U>
{
    T find(List<T> components, U component);
}