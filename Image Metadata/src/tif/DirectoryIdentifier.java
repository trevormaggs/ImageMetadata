package tif;

/**
 * This program enumerates Image File Directory (IFD) types typically found within TIFF image files.
 *
 * <p>
 * Change History:
 * </p>
 * 
 * <ul>
 * <li>Version 1.0 - Initial release by Trevor Maggs on 21 June 2025</li>
 * </ul>
 * 
 * @version 0.1
 * @author Trevor Maggs, trevmaggs@tpg.com.au
 * @since 21 June 2025
 */
public enum DirectoryIdentifier
{
    TIFF_DIRECTORY_IFD0(0, "IFD0"),
    TIFF_DIRECTORY_IFD1(1, "IFD1"),
    TIFF_DIRECTORY_IFD2(2, "IFD2"),
    TIFF_DIRECTORY_IFD3(3, "IFD3"),
    TIFF_DIRECTORY_SUBIFD(10, "SubIFD"),
    TIFF_DIRECTORY_SUBIFD1(11, "SubIFD1"),
    TIFF_DIRECTORY_SUBIFD2(12, "SubIFD2"),
    TIFF_DIRECTORY_SUBIFD3(13, "SubIFD3"),
    EXIF_DIRECTORY_SUBIFD(20, "Exif SubIFD"),
    EXIF_DIRECTORY_GPS(30, "GPS IFD"),
    EXIF_DIRECTORY_INTEROP(40, "Interop IFD"),
    EXIF_DIRECTORY_MAKER_NOTES(50, "Maker Notes"),
    TIFF_DIRECTORY_UNKNOWN(99, "Unknown");

    public static final int MAX_IFD_OR_SUBIFD = 4;
    private final int directoryTypeIndex;
    private final String description;

    /**
     * This private constructor is implicitly called to initialise every directory with a distinct
     * enumeration value given.
     *
     * @param directoryType
     *        the unique identifier number assigned to the respective directory
     * @param description
     *        the name that describes the specified directory
     */
    private DirectoryIdentifier(final int directoryType, final String description)
    {
        this.directoryTypeIndex = directoryType;
        this.description = description;
    }

    /**
     * Gets the type index which the directory is identified with.
     * 
     * @return the index number
     */
    public int getDirectoryTypeIndex()
    {
        return directoryTypeIndex;
    }

    /**
     * Gets the description of the directory.
     * 
     * @return the name of the Directory, for example, IFD0, IFD1 etc
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Retrieves the IFD structure corresponding to the specified directory index.
     *
     * @param index
     *        the ID number to find the corresponding directory
     *
     * @return the DirectoryIdentifier that matches the specified index, otherwise
     *         TIFF_DIRECTORY_UNKNOWN is returned
     */
    public static DirectoryIdentifier getDirectoryType(int index)
    {
        for (DirectoryIdentifier tifDirType : values())
        {
            if (tifDirType.directoryTypeIndex == index)
            {
                return tifDirType;
            }
        }

        return TIFF_DIRECTORY_UNKNOWN;
    }

    /**
     * Finds the next available IFD structure from the current directory. For instance, going from
     * IFD0 to IFD1. At the time of its development, it only supports up to IFD4 maximum.
     *
     * @param dirType
     *        the type of directory being read
     *
     * @return the next available DirectoryIdentifier, or the current DirectoryIdentifier if the
     *         maximum IFD number has been reached
     */
    public static DirectoryIdentifier getNextDirectoryType(DirectoryIdentifier dirType)
    {
        int nextIdx = (dirType.directoryTypeIndex % 10) + 1;

        if (nextIdx < MAX_IFD_OR_SUBIFD)
        {
            return getDirectoryType(nextIdx);
        }

        else
        {
            return dirType;
        }
    }
}