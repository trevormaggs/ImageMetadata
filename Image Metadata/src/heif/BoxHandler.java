package heif;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import common.BaseMetadata;
import common.ImageHandler;
import common.ImageReadErrorException;
import common.Metadata;
import common.SequentialByteReader;
import heif.boxes.*;
import heif.boxes.ItemLocationBox.ExtentData;
import logger.LogFactory;
import tif.TifParser;

/**
 * Handles parsing of HEIF/HEIC file structures based on the ISO Base Media Format.
 *
 * Supports Exif extraction, box navigation, and hierarchical parsing.
 * 
 * <p>
 * For detailed specifications, see:
 * </p>
 * 
 * <ul>
 * <li>{@code ISO/IEC 14496-12:2015}</li>
 * <li>{@code ISO/IEC 23008-12:2017}</li>
 * </ul>
 *
 * @apiNote According to HEIF/HEIC standards, some box types are optional and may appear zero or one
 *          time per file.
 * 
 * @author Trevor Maggs
 * @since 17 June 2025
 */
public class BoxHandler implements ImageHandler
{
    private static final LogFactory LOGGER = LogFactory.getLogger(BoxHandler.class);

    private final Map<HeifBoxType, List<Box>> heifBoxMap;
    private final SequentialByteReader reader;
    private final Path imageFile;

    /**
     * This default constructor should not be invoked, or it will throw an exception to prevent
     * instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    public BoxHandler()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Constructs a {@code BoxHandler} using a file path and byte reader and begins the parsing of
     * the HEIF file.
     *
     * @param fpath
     *        the path to the HEIF file
     * @param reader
     *        the {@code SequentialByteReader} for reading file content
     */
    public BoxHandler(Path fpath, SequentialByteReader reader)
    {
        this.imageFile = fpath;
        this.reader = reader;
        this.heifBoxMap = new LinkedHashMap<HeifBoxType, List<Box>>();
    }

    /**
     * Constructs a {@code BoxHandler} using raw byte data.
     *
     * @param fpath
     *        the path to the HEIF file
     * @param payload
     *        the raw file data as byte array
     */
    public BoxHandler(Path fpath, byte[] payload)
    {
        this(fpath, new SequentialByteReader(payload));
    }

    /**
     * Parses all HEIF boxes from the file stream and populates the internal box map.
     */
    public void parse()
    {
        while (reader.getCurrentPosition() < reader.length())
        {
            long startPos = reader.getCurrentPosition();
            Box box = BoxFactory.createBox(reader);

            /*
             * TODO: We don't know how the data within the Media Data box can be effectively handled
             * and since we are not really interested in parsing it, it will just be skipped.
             * However, once a handler is developed, this should take care of that box correctly,
             * hopefully.
             */
            if (HeifBoxType.BOX_MEDIA_DATA.matchesBoxName(box.getBoxName()))
            {
                LOGGER.warn("Skipping unhandled Media Data box [" + box.getBoxName() + "] at offset [" + startPos + "]");
                break;
            }

            heifBoxMap.putIfAbsent(box.getHeifType(), new ArrayList<Box>());
            heifBoxMap.get(box.getHeifType()).add(box);

            List<Box> children = box.addBoxList();

            if (children != null)
            {
                for (Box child : children)
                {
                    heifBoxMap.putIfAbsent(child.getHeifType(), new ArrayList<Box>());
                    heifBoxMap.get(child.getHeifType()).add(child);
                }
            }
        }
    }

    /**
     * Extracts the raw Exif data block from the HEIF container.
     *
     * <p>
     * The returned byte array starts at the TIFF header and excludes the Exif identifier prefix.
     * </p>
     *
     * @return a byte array containing the TIFF-compatible Exif block
     *
     * @throws ImageReadErrorException
     *         if the Exif block is missing, malformed, or cannot be located
     */
    public byte[] getExifBlock() throws ImageReadErrorException
    {
        ItemInformationBox iinf = getIINF();
        ItemLocationBox iloc = getILOC();

        if (iinf == null || !iinf.hasExifBlock())
        {
            throw new ImageReadErrorException("Exif block not found in Item Information Box for [" + imageFile + "]");
        }

        int exifID = iinf.getExifID();
        List<ExtentData> extents = (iloc != null) ? iloc.findExtentDataList(exifID) : null;

        if (extents == null || extents.isEmpty())
        {
            throw new ImageReadErrorException("Item Location Box missing or no entry for Exif ID [" + exifID + "]");
        }

        int totalLength = 0;
        List<byte[]> parts = new ArrayList<byte[]>();

        for (ExtentData extent : extents)
        {
            reader.mark();
            reader.seek(extent.getExtentOffset());

            if (extent.getExtentLength() < 8)
            {
                throw new ImageReadErrorException("Extent too small to contain Exif header in [" + imageFile + "]");
            }

            int exifTiffHeaderOffset = reader.readInteger();

            if (extent.getExtentLength() < exifTiffHeaderOffset + 4)
            {
                throw new ImageReadErrorException("Invalid TIFF header offset for Exif block in [" + imageFile + "]");
            }

            reader.skip(exifTiffHeaderOffset);
            byte[] exifPart = reader.peek(reader.getCurrentPosition(), extent.getExtentLength() - exifTiffHeaderOffset - 4);
            reader.reset();

            parts.add(exifPart);
            totalLength += exifPart.length;
        }

        int pos = 0;
        byte[] exifData = new byte[totalLength];

        for (byte[] part : parts)
        {
            System.arraycopy(part, 0, exifData, pos, part.length);
            pos += part.length;
        }

        return exifData;
    }

    /**
     * Retrieves the first matching box of a specific type and class.
     *
     * @param <T>
     *        the generic box type
     * @param type
     *        the box type identifier
     * @param clazz
     *        the expected box class
     *
     * @return the matching box, or {@code null} if not present or of the wrong type
     */
    @SuppressWarnings("unchecked")
    private <T extends Box> T getBox(HeifBoxType type, Class<T> clazz)
    {
        List<Box> boxes = heifBoxMap.get(type);

        if (boxes != null)
        {
            for (Box box : boxes)
            {
                if (clazz.isInstance(box))
                {
                    return (T) box;
                }
            }
        }

        return null;
    }

    /**
     * Gets the {@link HandlerBox}, if present.
     *
     * @return the {@code HandlerBox}, or {@code null} if not found
     */
    public HandlerBox getHDLR()
    {
        return getBox(HeifBoxType.BOX_HANDLER, HandlerBox.class);
    }

    /**
     * Gets the {@link PrimaryItemBox}, if present.
     *
     * @return the {@code PrimaryItemBox}, or {@code null} if not found
     */
    public PrimaryItemBox getPITM()
    {
        return getBox(HeifBoxType.BOX_PRIMARY_ITEM, PrimaryItemBox.class);
    }

    /**
     * Gets the {@link ItemInformationBox}, if present.
     *
     * @return the {@code ItemInformationBox}, or {@code null} if not found
     */
    public ItemInformationBox getIINF()
    {
        return getBox(HeifBoxType.BOX_ITEM_INFO, ItemInformationBox.class);
    }

    /**
     * Gets the {@link ItemLocationBox}, if present.
     *
     * @return the {@code ItemLocationBox}, or {@code null} if not found
     */
    public ItemLocationBox getILOC()
    {
        return getBox(HeifBoxType.BOX_ITEM_LOCATION, ItemLocationBox.class);
    }

    /**
     * Gets the {@link ItemPropertiesBox}, if present.
     *
     * @return the {@code ItemPropertiesBox}, or {@code null} if not found
     */
    public ItemPropertiesBox getIPRP()
    {
        return getBox(HeifBoxType.BOX_IMAGE_PROPERTY, ItemPropertiesBox.class);
    }

    /**
     * Gets the {@link ItemReferenceBox}, if present.
     *
     * @return the {@code ItemReferenceBox}, or {@code null} if not found
     */
    public ItemReferenceBox getIREF()
    {
        return getBox(HeifBoxType.BOX_ITEM_REFERENCE, ItemReferenceBox.class);
    }

    /**
     * Gets the {@link ItemDataBox}, if present.
     *
     * @return the {@code ItemDataBox}, or {@code null} if not found
     */
    public ItemDataBox getIDAT()
    {
        return getBox(HeifBoxType.BOX_ITEM_DATA, ItemDataBox.class);
    }

    /**
     * Returns the parsed box map.
     * 
     * @return the map of box lists, keyed by HeifBoxType
     */
    public Map<HeifBoxType, List<Box>> getBoxes()
    {
        return heifBoxMap;
    }

    /**
     * Extracts Exif metadata from the HEIF file.
     *
     * @return a {@link Metadata} object containing the parsed Exif data
     *
     * @throws IOException
     *         if an I/O error occurs
     * @throws ImageReadErrorException
     *         if Exif data cannot be located or is invalid
     */
    @Override
    public Metadata<? extends BaseMetadata> processMetadata() throws IOException, ImageReadErrorException
    {
        parse();
        
        byte[] exifData = getExifBlock();

        return new TifParser(imageFile, exifData).getMetadata();
    }
}