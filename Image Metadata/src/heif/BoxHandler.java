package heif;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import common.ImageHandler;
import common.ImageReadErrorException;
import common.SequentialByteReader;
import heif.boxes.Box;
import heif.boxes.DataInformationBox;
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
 * <p>
 * <strong>API Note:</strong> According to HEIF/HEIC standards, some box types are
 * optional and may appear zero or one time per file.
 * </p>
 *
 * @author Trevor Maggs
 * @since 25 June 2025
 */
public class BoxHandler implements ImageHandler, Iterable<Box>
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
     *
     * @throws NullPointerException
     *         if any argument is null
     */
    public BoxHandler(Path fpath, SequentialByteReader reader)
    {
        this.reader = reader;
        this.imageFile = fpath;
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
        return getBox(HeifBoxType.HANDLER, HandlerBox.class);
    }

    /**
     * Gets the {@link PrimaryItemBox}, if present.
     *
     * @return the {@code PrimaryItemBox}, or {@code null} if not found
     */
    public PrimaryItemBox getPITM()
    {
        return getBox(HeifBoxType.PRIMARY_ITEM, PrimaryItemBox.class);
    }

    /**
     * Gets the {@link ItemInformationBox}, if present.
     *
     * @return the {@code ItemInformationBox}, or {@code null} if not found
     */
    public ItemInformationBox getIINF()
    {
        return getBox(HeifBoxType.ITEM_INFO, ItemInformationBox.class);
    }

    /**
     * Gets the {@link ItemLocationBox}, if present.
     *
     * @return the {@code ItemLocationBox}, or {@code null} if not found
     */
    public ItemLocationBox getILOC()
    {
        return getBox(HeifBoxType.ITEM_LOCATION, ItemLocationBox.class);
    }

    /**
     * Gets the {@link ItemPropertiesBox}, if present.
     *
     * @return the {@code ItemPropertiesBox}, or {@code null} if not found
     */
    public ItemPropertiesBox getIPRP()
    {
        return getBox(HeifBoxType.ITEM_PROPERTIES, ItemPropertiesBox.class);
    }

    /**
     * Gets the {@link ItemReferenceBox}, if present.
     *
     * @return the {@code ItemReferenceBox}, or {@code null} if not found
     */
    public ItemReferenceBox getIREF()
    {
        return getBox(HeifBoxType.ITEM_REFERENCE, ItemReferenceBox.class);
    }

    /**
     * Gets the {@link ItemDataBox}, if present.
     *
     * @return the {@code ItemDataBox}, or {@code null} if not found
     */
    public ItemDataBox getIDAT()
    {
        return getBox(HeifBoxType.ITEM_DATA, ItemDataBox.class);
    }

    /**
     * Gets the {@link DataInformationBox}, if present.
     *
     * @return the {@code DataInformationBox}, or {@code null} if not found
     */
    public DataInformationBox getDINF()
    {
        return getBox(HeifBoxType.DATA_INFORMATION, DataInformationBox.class);
    }

    /**
     * Returns the parsed box map.
     *
     * @return the map of box lists, keyed by HeifBoxType
     */
    public Map<HeifBoxType, List<Box>> getBoxes()
    {
        return Collections.unmodifiableMap(heifBoxMap);
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
     *         present, otherwise, {@link Optional#empty()}
     *
     * @throws ImageReadErrorException
     *         if the Exif block is missing, malformed, or cannot be located
     */
    public Optional<byte[]> getExifData() throws ImageReadErrorException
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
     *
     * @throws ImageReadErrorException
     *         if the file contains incorrect magic numbers
     * @throws IOException
     *         if the magic numbers cannot be determined
     */
    @Override
    public boolean parseMetadata() throws ImageReadErrorException, IOException
    {
        parse();
        return (heifBoxMap.size() > 0);
    }

    /**
     * Returns a depth-first iterator over all parsed boxes, starting from root boxes.
     *
     * <p>
     * The iteration respects the hierarchy of boxes, processing children before siblings
     * (depth-first traversal).
     * </p>
     *
     * @return an {@link Iterator} for recursively visiting all boxes
     */
    @Override
    public Iterator<Box> iterator()
    {
        return new Iterator<Box>()
        {
            private final Deque<Box> stack = new ArrayDeque<>();

            {
                List<Box> roots = getTopLevelBoxes();

                // Add roots in reverse order to maintain original sequence
                for (int i = roots.size() - 1; i >= 0; i--)
                {
                    stack.push(roots.get(i));
                }
            }

            @Override
            public boolean hasNext()
            {
                return !stack.isEmpty();
            }

            @Override
            public Box next()
            {
                Box current = stack.pop();
                List<Box> children = current.getBoxList();

                if (children != null)
                {
                    for (int i = children.size() - 1; i >= 0; i--)
                    {
                        stack.push(children.get(i));
                    }
                }

                return current;
            }
        };
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
     * Retrieves the list of {@link ExtentData} corresponding to the Exif block, if present.
     *
     * @return an {@link Optional} containing the list of extents for Exif data, or
     *         {@link Optional#empty()} if it does not exist
     */
    private Optional<List<ExtentData>> getExifExtents()
    {
        List<ExtentData> extents = null;
        ItemLocationBox iloc = getILOC();
        ItemInformationBox iinf = getIINF();

        if (iinf == null)
        {
            LOGGER.warn("Item Information Box is missing in file [" + imageFile + "]");
            return Optional.empty();
        }

        else if (!iinf.containsExif())
        {
            LOGGER.warn("Item Information Box in [" + imageFile + "] does not contain Exif data");
            return Optional.empty();
        }

        int exifID = iinf.findExifItemID();

        if (iloc != null)
        {
            extents = iloc.findExtentsForItem(exifID);
        }

        if (extents == null || extents.isEmpty())
        {
            LOGGER.warn("Item Location Box missing or no entry for Exif ID [" + exifID + "]");
            return Optional.empty();
        }

        return Optional.of(extents);
    }

    /**
     * Displays all box types in a hierarchical fashion, useful for debugging, visualisation or
     * diagnostics.
     */
    public void displayHierarchy()
    {
        for (Box box : this)
        {
            int depth = 0;
            Box parent = box.getParent();

            while (parent != null)
            {
                depth++;
                parent = parent.getParent();
            }

            for (int i = 0; i < depth; i++)
            {
                System.out.print("\t");
            }

            System.out.println(box.getTypeAsString());
        }
    }

    /**
     * Parses all HEIF boxes from the file stream using {@link BoxFactory#createBox} and builds the
     * internal box tree structure.
     *
     * <p>
     * This method skips un-handled types such as {@code mdat} and gracefully recovers from
     * malformed boxes using a fail-fast approach.
     * </p>
     */
    private void parse()
    {
        do
        {
            try
            {
                Box box = BoxFactory.createBox(reader);

                /*
                 * At this stage, no handler for processing data within the Media Data box (mdat) is
                 * available, since we are not interested in parsing it yet. This box will be
                 * skipped as not handled. Often, mdat is the last top-level box.
                 *
                 * TODO: work out how mdat data can be handled.
                 */
                if (HeifBoxType.MEDIA_DATA.equalsTypeName(box.getTypeAsString()))
                {
                    reader.skip(box.available());
                    LOGGER.warn("Skipping unhandled Media Data box [" + box.getTypeAsString() + "]");
                }

                walkBoxes(box, 0);
            }

            /*
             * Just in case, it is better to catch a general Exception for
             * robustness during parsing and exit, ie corrupted files
             */
            catch (Exception exc)
            {
                LOGGER.error("Failed to parse box: [" + exc.getMessage() + "]");
                break;
            }

        } while (reader.getCurrentPosition() < reader.length());
    }

    /**
     * Recursively traverses the HEIF box hierarchy, adding each encountered box to the internal
     * {@code heifBoxMap}.
     *
     * <p>
     * This method is used internally by the {@link #parse()} method to build a comprehensive map of
     * all boxes and their relationships within the HEIF file.
     * </p>
     *
     * @param box
     *        the current {@link Box} object to process. This box and its children will be added to
     *        the internal map
     * @param depth
     *        the current depth in the box hierarchy, primarily used for debugging/visualisation
     *        purposes
     */
    private void walkBoxes(Box box, int depth)
    {
        heifBoxMap.putIfAbsent(box.getHeifType(), new ArrayList<>());
        heifBoxMap.get(box.getHeifType()).add(box);

        List<Box> children = box.getBoxList();

        if (children != null)
        {
            for (Box child : children)
            {
                child.setParent(box);
                walkBoxes(child, depth + 1);
            }
        }
    }

    /**
     * Returns all top-level (root) boxes that do not have a parent.
     *
     * <p>
     * This method dynamically reconstructs the top-level box list from the parsed box map,
     * eliminating the need for a separate root tracking structure.
     * </p>
     *
     * @return a list of root {@link Box} instances in parsing order
     */
    private List<Box> getTopLevelBoxes()
    {
        List<Box> roots = new ArrayList<>();

        for (List<Box> list : heifBoxMap.values())
        {
            for (Box box : list)
            {
                if (box.getParent() == null)
                {
                    roots.add(box);
                }
            }
        }

        return roots;
    }
}