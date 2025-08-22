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
import java.util.Arrays;
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
 * This {@code IFDHandler} parses TIFF-based files by reading and interpreting Image File
 * Directories (IFDs) within the file's binary structure.
 * 
 * <p>
 * It supports standard TIFF 6.0 parsing, including IFD0, EXIF, GPS, and INTEROP directories,
 * traversed recursively via tag-defined pointers.
 * </p>
 * 
 * <p>
 * <strong>Note:</strong> BigTIFF (version 43) is detected but not supported.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 22 August 2025
 * @see <a href="https://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf">TIFF 6.0
 *      Specification (Adobe) for in-depth technical information</a>
 */
public class IFDHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(IFDHandler.class);
    private static final int TIFF_STANDARD_VERSION = 42;
    private static final int TIFF_BIG_VERSION = 43;
    public static final int ENTRY_MAX_VALUE_LENGTH = 4;
    public static final int ENTRY_MAX_VALUE_LENGTH_BIG = 8; // reserved for BigTIFF

    private static final List<Class<? extends Enum<?>>> tagClassList;
    private static final Map<Taggable, DirectoryIdentifier> subIfdMap;
    private final List<DirectoryIFD> directoryList;
    private final SequentialByteReader reader;
    private boolean isTiffBig;
    private int firstIFDoffset;
    private int tifHeaderOffset;

    private static final Map<Integer, Taggable> TAG_LOOKUP;

    static
    {
        tagClassList = Collections.unmodifiableList(Arrays.asList(
                TagEXIF.class,
                TagGPS.class,
                TagIFD.class,
                TagINTEROP.class,
                TagSUBIFD.class));

        subIfdMap = Collections.unmodifiableMap(new HashMap<Taggable, DirectoryIdentifier>()
        {
            {
                put(IFD_TAG_IFD_POINTER, EXIF_DIRECTORY_SUBIFD);
                put(IFD_TAG_EXIF_POINTER, EXIF_DIRECTORY_SUBIFD);
                put(IFD_TAG_GPS_INFO_POINTER, EXIF_DIRECTORY_GPS);
                put(EXIF_TAG_INTEROP_POINTER, EXIF_DIRECTORY_INTEROP);
            }
        });

        Map<Integer, Taggable> map = new HashMap<>();

        for (Class<? extends Enum<?>> enumClass : tagClassList)
        {
            for (Enum<?> val : enumClass.getEnumConstants())
            {
                Taggable tag = (Taggable) val;
                map.put(tag.getNumberID(), tag);
            }
        }

        TAG_LOOKUP = Collections.unmodifiableMap(map);
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
     * Currently not implemented.
     *
     * @return always throws an exception
     */
    @Override
    public long getSafeFileSize()
    {
        throw new UnsupportedOperationException("Not implemented yet");
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
     */
    @Override
    public boolean parseMetadata()
    {
        if (!readTifHeader())
        {
            LOGGER.error("Invalid TIFF header detected. Metadata parsing cancelled");
            return false;
        }

        navigateImageFileDirectory(DirectoryIdentifier.TIFF_DIRECTORY_IFD0, tifHeaderOffset + firstIFDoffset);

        return (!directoryList.isEmpty());
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
     * Reads the TIFF header to determine byte order, version (Standard or BigTIFF), and the offset
     * to the first Image File Directory (IFD0). Note, at this stage, the BigTIFF configuration is
     * detectable but it is not fully supported yet.
     *
     * @return true if the TIFF header check is passed, otherwise false if malformed
     */
    private boolean readTifHeader()
    {
        byte firstByte = reader.readByte();
        byte secondByte = reader.readByte();

        isTiffBig = true;

        if (firstByte == secondByte)
        {
            if (firstByte == 0x49)
            {
                reader.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                LOGGER.debug("Byte order detected as [Intel]");
            }

            else if (firstByte == 0x4D)
            {
                reader.setByteOrder(ByteOrder.BIG_ENDIAN);
                LOGGER.debug("Byte order detected as [Motorola]");
            }

            else
            {
                LOGGER.warn("Unknown byte order [" + firstByte + "]");
                return false;
            }

            /* Identify whether this is Standard TIFF (42) or Big TIFF (43) version */
            int tiffVer = reader.readUnsignedShort();

            if (tiffVer == TIFF_BIG_VERSION)
            {
                /* TODO - develop and expand to support Big Tiff */
                LOGGER.warn("BigTIFF detected (not fully supported yet)");

                throw new UnsupportedOperationException("BigTIFF (version 43) not supported yet");
            }

            else if (tiffVer != TIFF_STANDARD_VERSION)
            {
                LOGGER.warn("Unexpected TIFF version [" + tiffVer + "], defaulting to standard TIFF 6.0");
                isTiffBig = false;
            }

            /* Advance by offset from base to IFD0 */
            firstIFDoffset = reader.readInteger();

            return true;
        }

        else
        {
            LOGGER.warn(String.format("Mismatched byte order bytes [First byte: 0x%04X ] and [Second byte: 0x%04X]", firstByte, secondByte));
        }

        return false;
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
     */
    private void navigateImageFileDirectory(DirectoryIdentifier dirType, long startOffset)
    {
        if (startOffset < 0 || startOffset >= reader.length())
        {
            LOGGER.warn("Invalid offset [" + startOffset + "] for directory [" + dirType + "]");
            return;
        }

        reader.seek(startOffset);
        DirectoryIFD ifd = new DirectoryIFD(dirType, reader.getByteOrder());
        int entryCount = reader.readUnsignedShort();

        for (int i = 0; i < entryCount; i++)
        {
            int tagID = reader.readUnsignedShort();
            Taggable tagEnum = TAG_LOOKUP.get(tagID);

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
                // Using long to prevent integer wraparound risk
                long end = (long) tifHeaderOffset + (long) offset + (long) totalBytes;

                if (end > reader.length() || end < 0)
                {
                    LOGGER.warn("Offset out of bounds for tag [" + tagEnum + "]");
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