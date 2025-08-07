package tif;

import static tif.DirectoryIdentifier.EXIF_DIRECTORY_GPS;
import static tif.DirectoryIdentifier.EXIF_DIRECTORY_INTEROP;
import static tif.DirectoryIdentifier.EXIF_DIRECTORY_SUBIFD;
import static tif.TagEntries.TagEXIF.EXIF_TAG_INTEROP_POINTER;
import static tif.TagEntries.TagIFD.IFD_TAG_EXIF_POINTER;
import static tif.TagEntries.TagIFD.IFD_TAG_GPS_INFO_POINTER;
import static tif.TagEntries.TagIFD.IFD_TAG_IFD_POINTER;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import common.ByteValueConverter;
import common.ImageHandler;
import common.SequentialByteReader;
import logger.LogFactory;
import tif.TagEntries.TagEXIF;
import tif.TagEntries.TagGPS;
import tif.TagEntries.TagIFD;
import tif.TagEntries.TagINTEROP;
import tif.TagEntries.TagSUBIFD;
import tif.TagEntries.Taggable;

/**
 * This {@code IFDHandler} class is responsible for parsing TIFF-based files by reading and
 * interpreting Image File Directories (IFDs) within the file's binary structure. It supports
 * standard TIFF parsing.
 *
 * This handler processes multiple TIFF directory types such as IFD0, EXIF, GPS, and INTEROP through
 * a recursive traversal of linked IFD structures identified by tag-defined pointers.
 *
 * <p>
 * <strong>Note:</strong> BigTIFF (version 43) is detected but not yet supported.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 25 June 2025
 * @see <a href="https://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf">TIFF 6.0
 *      Specification (Adobe) for in-depth technical information</a>
 */
public class IFDHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(IFDHandler.class);
    private static final int TIFF_STANDARD_VERSION = 42;
    private static final int TIFF_BIG_VERSION = 43;
    public static final int ENTRY_MAX_VALUE_LENGTH = 4;
    public static final int ENTRY_MAX_VALUE_LENGTH_BIG = 8;

    private static final List<Class<? extends Enum<?>>> tagClassList;
    private static final Map<Taggable, DirectoryIdentifier> subIfdMap;
    private final List<DirectoryIFD> directoryList;
    private final SequentialByteReader reader;
    private boolean isTiffBig;
    private int firstIFDoffset;
    private int tifHeaderOffset;

    static
    {
        tagClassList = new ArrayList<Class<? extends Enum<?>>>()
        {
            {
                add(TagEXIF.class);
                add(TagGPS.class);
                add(TagIFD.class);
                add(TagINTEROP.class);
                add(TagSUBIFD.class);
            }
        };

        subIfdMap = new HashMap<Taggable, DirectoryIdentifier>()
        {
            {
                put(IFD_TAG_IFD_POINTER, EXIF_DIRECTORY_SUBIFD);
                put(IFD_TAG_EXIF_POINTER, EXIF_DIRECTORY_SUBIFD);
                put(IFD_TAG_GPS_INFO_POINTER, EXIF_DIRECTORY_GPS);
                put(EXIF_TAG_INTEROP_POINTER, EXIF_DIRECTORY_INTEROP);
            }
        };
    }

    /**
     * Default constructor is unsupported and will always throw an exception.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    public IFDHandler()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Constructs an IFD handler for reading TIFF metadata using the specified byte reader.
     *
     * @param reader
     *        the byte reader providing access to the TIFF file content
     */
    public IFDHandler(SequentialByteReader reader)
    {
        this.reader = reader;
        this.tifHeaderOffset = 0;
        this.directoryList = new ArrayList<>();
    }

    /**
     * Indicates whether the parsed file is a BigTIFF variant (version 43).
     *
     * @return boolean true if the TIFF version is BigTIFF, otherwise false
     */
    public boolean isBigTiffVersion()
    {
        return isTiffBig;
    }

    /**
     * Parses the image data stream and attempts to extract metadata directories.
     *
     * <p>
     * After invoking this method, use {@link #getDirectories()} to retrieve the list of IFD
     * (Image File Directory) structures that were successfully parsed.
     * </p>
     *
     * @return true if at least one metadata directory was successfully extracted, otherwise false
     *
     * @throws IllegalStateException
     *         if the TIFF header is invalid or the stream data cannot be read correctly
     */
    @Override
    public boolean parseMetadata()
    {
        readTifHeader();
        navigateImageFileDirectory(DirectoryIdentifier.TIFF_DIRECTORY_IFD0, tifHeaderOffset + firstIFDoffset);

        return (directoryList.size() > 0);
    }

    /**
     * Returns the list of IFD directories that were successfully parsed.
     *
     * <p>
     * If no directories were found, this method returns {@link Optional#empty()}. Otherwise, it
     * returns an {@link Optional} containing a copy of the parsed IFD directory list.
     * </p>
     *
     * @return an {@link Optional} containing at least one {@link DirectoryIFD}, or
     *         {@link Optional#empty()} if no directories were parsed
     */
    public Optional<List<DirectoryIFD>> getDirectories()
    {
        return (directoryList.isEmpty() ? Optional.empty() : Optional.of(Collections.unmodifiableList(directoryList)));
    }

    /**
     * Retrieves the corresponding {@link Taggable} enumeration for the specified TIFF tag ID.
     *
     * @param tagid
     *        the tag ID to identify the field
     *
     * @return the resolved tag enum or {@code null} if unknown
     */
    private static Taggable getTagName(int tagid)
    {
        for (Class<? extends Enum<?>> enumClass : tagClassList)
        {
            for (Enum<?> val : enumClass.getEnumConstants())
            {
                Taggable tag = (Taggable) val;

                if (tagid == tag.getNumberID())
                {
                    return tag;
                }
            }
        }

        return null;
    }

    /**
     * Reads the TIFF header to determine byte order, version (Standard or BigTIFF), and the offset
     * to the first Image File Directory (IFD0).
     *
     * @throws IllegalStateException
     *         if the byte order is invalid or unsupported
     */
    private void readTifHeader()
    {
        byte firstByte = reader.readByte();
        byte secondByte = reader.readByte();

        if (firstByte == secondByte)
        {
            if (firstByte == 0x49)
            {
                reader.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                LOGGER.info("Byte order detected as [Intel]");
            }

            else if (firstByte == 0x4D)
            {
                reader.setByteOrder(ByteOrder.BIG_ENDIAN);
                LOGGER.info("Byte order detected as [Motorola]");
            }

            else
            {
                throw new IllegalStateException("Unknown byte order: [" + firstByte + "]");
            }

            /* Identify whether this is Standard TIFF (42) or Big TIFF (43) version */
            int tiffVer = reader.readUnsignedShort();

            if (tiffVer == TIFF_BIG_VERSION)
            {
                isTiffBig = true;
                LOGGER.warn("BigTIFF detected (not fully supported yet)");
            }

            else if (tiffVer != TIFF_STANDARD_VERSION)
            {
                LOGGER.warn("Unexpected TIFF version [" + tiffVer + "], defaulting to standard TIFF 6.0");
                isTiffBig = false;
            }

            /* Advance by offset from base to IFD0 */
            firstIFDoffset = reader.readInteger();
        }

        else
        {
            throw new IllegalStateException("Mismatched byte order bytes: [First byte: 0x" + Integer.toHexString(firstByte).toUpperCase() + "] and [Second byte: 0x" + Integer.toHexString(secondByte).toUpperCase() + "]");
        }
    }

    /**
     * Recursively traverses the specified Image File Directory and its linked sub-directories based
     * on the tag-defined pointers, either EXIF, GPS or Interop).
     *
     * For comprehensive technical context, refer to the TIFF Specification Revision 6.0 document on
     * Page 13 to 16.
     *
     * @param dirType
     *        the directory type being processed
     * @param startOffset
     *        the file offset (from header base) where the IFD begins
     * 
     * @throws IllegalStateException
     *         if there is a problem during reading the stream data
     */
    private void navigateImageFileDirectory(DirectoryIdentifier dirType, long startOffset)
    {
        if (startOffset < 0 || startOffset >= reader.length())
        {
            LOGGER.error("Invalid offset [" + startOffset + "] for directory [" + dirType + "]");
            return;
        }

        reader.seek(startOffset);
        DirectoryIFD ifd = new DirectoryIFD(dirType, reader.getByteOrder());
        int entryCount = reader.readUnsignedShort();

        for (int i = 0; i < entryCount; i++)
        {
            int tagID = reader.readUnsignedShort();
            Taggable tagEnum = getTagName(tagID);

            /*
             * To address rare instances where tag IDs are found to be undefined,
             * this part will skip and continue to the next iteration.
             */
            if (tagEnum == null)
            {
                LOGGER.warn("Unknown tag ID: 0x" + Integer.toHexString(tagID));
                reader.skip(10); // skip rest of the entry
                continue;
            }

            TifFieldType fieldType = TifFieldType.getTiffType(reader.readUnsignedShort());
            int count = (int) reader.readUnsignedInteger();
            byte[] valueBytes = reader.readBytes(4);

            int offset = ByteValueConverter.toInteger(valueBytes, reader.getByteOrder());
            int totalBytes = count * fieldType.getElementLength();

            byte[] data;

            /*
             * A length of the value that is larger than 4 bytes indicates
             * the entry is an offset outside this directory field.
             */
            if (totalBytes > ENTRY_MAX_VALUE_LENGTH)
            {
                if ((tifHeaderOffset + offset + totalBytes) > reader.length())
                {
                    LOGGER.error("Offset out of bounds for tag [" + tagEnum + "]");
                    continue;
                }

                data = reader.peek(tifHeaderOffset + offset, totalBytes);
            }

            else
            {
                data = valueBytes;
            }

            /* Make sure the tag ID is known and defined in TIF Specification 6.0 */
            if (TifFieldType.dataTypeinRange(fieldType.getDataType()))
            {
                ifd.addEntry(tagEnum, fieldType, count, offset, data);
            }

            else
            {
                LOGGER.warn("Unknown field type [" + fieldType + "] for tag [" + tagEnum + "]");
                continue;
            }

            if (subIfdMap.containsKey(tagEnum))
            {
                reader.mark();
                navigateImageFileDirectory(subIfdMap.get(tagEnum), tifHeaderOffset + offset);
                reader.reset();
            }
        }

        directoryList.add(ifd);

        long nextOffset = reader.readUnsignedInteger();

        if (nextOffset != 0x0000L)
        {
            navigateImageFileDirectory(DirectoryIdentifier.getNextDirectoryType(dirType), tifHeaderOffset + nextOffset);
        }
    }
}