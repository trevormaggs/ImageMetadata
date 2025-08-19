package common;

/**
 * Represents a generic metadata container capable of storing and managing directory elements of
 * type {@code T}.
 *
 * <p>
 * This interface provides a simplified abstraction for working with metadata structures across
 * different image formats, including PNG, TIFF, JPEG, etc. It includes common operations such as
 * checking for metadata presence, adding directories, and producing debug-friendly output. Support
 * for use by both composite and leaf-style metadata implementations.
 * </p>
 *
 * @param <T>
 *        the type of metadata directory stored
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public interface Metadata<T> extends BaseMetadata, Iterable<T>
{
    /**
     * Adds a metadata directory to this container.
     *
     * @param directory
     *        the metadata directory to add, must not be null
     */
    public void addDirectory(T directory);

    /**
     * Retrieves a metadata directory that matches the provided component descriptor.
     *
     * <p>
     * This method supports multiple forms of lookup depending on the implementation, such as:
     * </p>
     *
     * <ul>
     * <li>Tag-based lookup (for example, {@code Taggable}, {@code TextKeyword})</li>
     * <li>Directory identifier (for example, {@code DirectoryIdentifier})</li>
     * <li>Type-based or instance-based queries</li>
     * </ul>
     *
     * @param <U>
     *        the type of the component used as a lookup key
     * @param component
     *        the lookup reference or identifying element
     *
     * @return the matching directory, or null if no match is found
     */
    public <U> T getDirectory(U component);

    /**
     * Returns {@code true} if the container holds no metadata directories.
     *
     * @return true if empty, otherwise false
     */
    public boolean isEmpty();

    /**
     * Checks if the container holds any metadata.
     *
     * @return true if any directory is present, otherwise false
     */
    public boolean hasMetadata();

    /**
     * Determines whether this container holds any Exif metadata via TIFF/IFD structures.
     *
     * @return true if Exif data is available, otherwise false
     */
    public boolean hasExifData();
}