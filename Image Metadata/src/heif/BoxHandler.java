package heif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import common.ImageHandler;
import common.ImageReadErrorException;
import common.SequentialByteReader;
import heif.boxes.Box;
import heif.boxes.HandlerBox;
import heif.boxes.ItemDataBox;
import heif.boxes.ItemInformationBox;
import heif.boxes.ItemLocationBox;
import heif.boxes.ItemLocationBox.ExtentData;
import heif.boxes.ItemPropertiesBox;
import heif.boxes.ItemReferenceBox;
import heif.boxes.PrimaryItemBox;
import logger.LogFactory;

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
    private final List<Box> topLevelBoxes = new ArrayList<>();

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
        this.heifBoxMap = new LinkedHashMap<>();
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
     * Extracts the embedded Exif TIFF block from the HEIF container.
     *
     * <p>
     * Supports multi-extent Exif data and correctly applies the TIFF header offset as specified in
     * the Exif payload. If no Exif block is found, {@link Optional#empty()} is returned.
     * </p>
     *
     * <p>
     * The returned byte array starts at the TIFF header, excluding the standard Exif identifier
     * prefix (usually {@code Exif\0\0}). According to <b>ISO/IEC 23008-12:2017 Annex A (p. 37)</b>,
     * the first 4 bytes of the Exif item payload contain {@code exifTiffHeaderOffset}, which
     * specifies the offset from the start of the payload to the TIFF header.
     * </p>
     *
     * <p>
     * The TIFF header typically begins with two magic bytes indicating byte order:
     * </p>
     *
     * <ul>
     * <li>{@code 0x4D 0x4D} – Motorola (big-endian)</li>
     * <li>{@code 0x49 0x49} – Intel (little-endian)</li>
     * </ul>
     *
     * @return an {@link Optional} containing the TIFF-compatible Exif block as a byte array if
     *         present, otherwise, {@link Optional#empty()}.
     *
     * @throws ImageReadErrorException
     *         if the Exif block is missing, malformed, or cannot be located.
     */
    public Optional<byte[]> getExifBlock() throws ImageReadErrorException
    {
        Optional<List<ExtentData>> optionalExif = getExifExtents();

        if (optionalExif.isPresent())
        {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
            {
                boolean isFirstExtent = true;

                for (ExtentData extent : optionalExif.get())
                {
                    reader.mark();
                    reader.seek(extent.getExtentOffset());

                    if (isFirstExtent)
                    {
                        isFirstExtent = false;

                        if (extent.getExtentLength() < 8)
                        {
                            throw new ImageReadErrorException("Extent too small to contain Exif header in [" + imageFile + "]");
                        }

                        int exifTiffHeaderOffset = reader.readInteger();

                        if (extent.getExtentLength() < exifTiffHeaderOffset + 4)
                        {
                            throw new ImageReadErrorException("Invalid TIFF header offset for Exif block in [" + imageFile + "]");
                        }

                        /*
                         * The Exif payload begins at the position indicated by
                         * exifTiffHeaderOffset. We skip this offset to locate the TIFF header and
                         * subtract 4 bytes from the remaining length to exclude the offset field
                         * itself from the Exif data.
                         */
                        reader.skip(exifTiffHeaderOffset);

                        int payloadLength = (extent.getExtentLength() - exifTiffHeaderOffset - 4);

                        baos.write(reader.peek(reader.getCurrentPosition(), payloadLength));
                    }

                    else
                    {
                        baos.write(reader.peek(reader.getCurrentPosition(), extent.getExtentLength()));
                    }

                    reader.reset();
                }

                return (baos.size() > 0 ? Optional.of(baos.toByteArray()) : Optional.empty());
            }

            catch (IOException exc)
            {
                throw new ImageReadErrorException("Unable to process Exif block: [" + exc.getMessage() + "]", exc);
            }
        }

        return Optional.empty();
    }

    /**
     * Parses the image data stream and attempts to extract metadata from the HEIF container.
     *
     * <p>
     * After calling this method, you can retrieve the extracted Exif block (if present) by invoking
     * {@link #getExifBlock()}.
     * </p>
     *
     * @return true if at least one HEIF box was successfully parsed and extracted, or false if no
     *         relevant boxes were found
     */
    @Override
    public boolean parseMetadata()
    {
        parse();

        for (List<Box> list : heifBoxMap.values())
        {
            for (Box box : list)
            {
                System.out.printf("%s\n", box.getTypeAsString());
            }
        }

        return (heifBoxMap.size() > 0);
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
     * Parses all HEIF boxes from the file stream and populates the internal box map.
     */
    private void parse()
    {
        while (reader.getCurrentPosition() < reader.length())
        {
            long startPos = reader.getCurrentPosition();
            Box box = BoxFactory.createBox(reader);

            /*
             * At this stage, no handler for processing data within the Media Data box (mdat) is
             * available, since we are not interested in parsing it yet. This box will be skipped as
             * un-handled.
             *
             * TODO: work out how mdat data can be handled.
             */
            if (HeifBoxType.BOX_MEDIA_DATA.matchesBoxName(box.getTypeAsString()))
            {
                LOGGER.warn("Skipping unhandled Media Data box [" + box.getTypeAsString() + "] at offset [" + startPos + "]");
                break;
            }

            heifBoxMap.putIfAbsent(box.getHeifType(), new ArrayList<>());
            heifBoxMap.get(box.getHeifType()).add(box);

            List<Box> children = box.getBoxList();

            if (children != null)
            {
                for (Box child : children)
                {
                    heifBoxMap.putIfAbsent(child.getHeifType(), new ArrayList<>());
                    heifBoxMap.get(child.getHeifType()).add(child);
                }
            }

            // topLevelBoxes.add(box);
            // flattenBox(box);
        }
    }

    private void flattenBox(Box box)
    {
        heifBoxMap.putIfAbsent(box.getHeifType(), new ArrayList<>());
        heifBoxMap.get(box.getHeifType()).add(box);

        List<Box> children = box.getBoxList();

        if (children != null)
        {
            for (Box child : children)
            {
                flattenBox(child);
            }
        }
    }

    private Optional<List<ExtentData>> getExifExtents()
    {
        ItemLocationBox iloc = getILOC();
        ItemInformationBox iinf = getIINF();

        if (iinf == null || !iinf.containsExif())
        {
            LOGGER.warn("Exif block not found in Item Information Box for [" + imageFile + "]");
            return Optional.empty();
        }

        int exifID = iinf.findExifItemID();
        List<ExtentData> extents = (iloc != null ? iloc.findExtentsForItem(exifID) : null);

        if (extents == null || extents.isEmpty())
        {
            LOGGER.warn("Item Location Box missing or no entry for Exif ID [" + exifID + "]");
            return Optional.empty();
        }

        return Optional.of(extents);
    }
}