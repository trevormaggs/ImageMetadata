package tif;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import common.Metadata;
import tif.DirectoryIFD.EntryIFD;

/**
 * A composite leaf component of the Composite design pattern that encapsulates Exif metadata stored
 * in TIFF-style directories. This class is typically used to manage and organise metadata extracted
 * from TIFF, JPEG, or PNG (via eXIf chunks) image files.
 *
 * <p>
 * It stores a map of {@link DirectoryIFD} instances, indexed by their {@link DirectoryIdentifier},
 * providing access, mutation, and query capabilities for structured image metadata.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 9 July 2025
 */
public class MetadataTIF implements Metadata<DirectoryIFD>
{
    private final Map<DirectoryIdentifier, DirectoryIFD> ifdMap;

    /**
     * Constructs an empty {@code MetadataTIF} container.
     */
    public MetadataTIF()
    {
        this.ifdMap = new HashMap<>();
    }

    /**
     * Adds a directory to the metadata map.
     *
     * @param directory
     *        the IFD directory to add
     */
    public void add(DirectoryIFD directory)
    {
        if (directory != null)
        {
            ifdMap.put(directory.getDirectoryType(), directory);
        }
    }

    /**
     * Removes a directory from the metadata map.
     *
     * @param directory
     *        the IFD directory to remove
     */
    public void remove(DirectoryIFD directory)
    {
        if (directory != null)
        {
            ifdMap.remove(directory.getDirectoryType());
        }
    }

    /**
     * Clears all metadata directories from this container.
     */
    public void clear()
    {
        ifdMap.clear();
    }

    /**
     * Checks whether a specific directory type is present.
     *
     * @param dir
     *        the directory identifier to check
     * 
     * @return true if the directory is stored; otherwise false
     */
    public boolean isDirectoryPresent(DirectoryIdentifier dir)
    {
        return ifdMap.containsKey(dir);
    }

    /**
     * Adds a directory to this metadata container.
     *
     * @param directory
     *        the directory to be added
     */
    @Override
    public void addDirectory(DirectoryIFD directory)
    {
        add(directory);
    }

    /**
     * Retrieves a directory from the metadata map using a provided lookup component.
     * 
     * <p>
     * If the component is a {@link DirectoryIdentifier}, a direct lookup is performed.
     * </p>
     *
     * @param <U>
     *        the type of the lookup key
     * @param component
     *        the lookup component (expected to be {@code DirectoryIdentifier})
     * 
     * @return the matching {@link DirectoryIFD}, or null if not found
     */
    @Override
    public <U> DirectoryIFD getDirectory(U component)
    {
        if (component instanceof DirectoryIdentifier)
        {
            return ifdMap.get(component);
        }

        return null;
    }

    /**
     * Returns whether this metadata container stores any directories.
     *
     * @return true if no directories are stored, otherwise false
     */
    @Override
    public boolean isEmpty()
    {
        return ifdMap.isEmpty();
    }

    /**
     * Returns whether this metadata container has any metadata.
     *
     * @return true if one or more directories exist
     */
    @Override
    public boolean hasMetadata()
    {
        return !isEmpty();
    }

    /**
     * Checks whether Exif metadata is present in this structure.
     *
     * @return true if the EXIF directory is present
     */
    @Override
    public boolean hasExifData()
    {
        return isDirectoryPresent(DirectoryIdentifier.EXIF_DIRECTORY_SUBIFD);
    }

    /**
     * Returns an iterator over all stored directories.
     *
     * @return an iterator of {@link DirectoryIFD} instances
     */
    @Override
    public Iterator<DirectoryIFD> iterator()
    {
        return ifdMap.values().iterator();
    }

    /**
     * Returns a simple concatenated string of all stored directories using their
     * {@code toString()} representations.
     *
     * @return a string containing all metadata directory values
     */
    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder();

        for (DirectoryIFD ifd : this)
        {
            result.append(ifd);
        }

        return result.toString();
    }

    /**
     * Produces a human-readable debug string summarising the contents of all directories and their
     * metadata entries. Useful for logging or diagnostic output.
     *
     * @param prefix
     *        an optional string to prepend as a heading or label. It may be {@code null}
     * 
     * @return a formatted string suitable for debugging, inspection, or textual analysis
     */
    @Override
    public String toString(String prefix)
    {
        String fmt = "%-12s:\t%s%n";
        String divider = "--------------------------------------------------";
        StringBuilder sb = new StringBuilder();

        if (prefix != null)
        {
            sb.append(prefix).append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }

        if (hasExifData())
        {
            sb.append("EXIF Metadata").append(System.lineSeparator());
            sb.append(divider).append(System.lineSeparator());

            for (DirectoryIFD ifd : this)
            {
                String label = ifd.getDirectoryType().getDescription();

                sb.append("\t\tDirectory - ");
                sb.append(label);
                sb.append(System.lineSeparator()).append(System.lineSeparator());

                for (EntryIFD entry : ifd)
                {
                    sb.append(String.format(fmt, "Tag Type", entry.getTag()));
                    sb.append(String.format("%-12s:\t0x%04X%n", "Tag ID", entry.getTagID()));
                    sb.append(String.format(fmt, "Field Type", entry.getFieldType()));
                    sb.append(String.format(fmt, "Count", entry.getCount()));
                    sb.append(String.format(fmt, "Value", ifd.getStringValue(entry)));
                    sb.append(System.lineSeparator());
                }
            }
        }

        else
        {
            sb.append("No metadata found.").append(System.lineSeparator());
        }

        return sb.toString();
    }
}