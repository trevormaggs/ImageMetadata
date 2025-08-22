package heif.boxes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import common.SequentialByteReader;
import heif.BoxFactory;
import heif.HeifBoxType;
import logger.LogFactory;

/**
 * Represents a MetaBox {@code meta} structure in HEIF/ISOBMFF files.
 *
 * The MetaBox contains metadata and subordinate boxes such as ItemInfoBox, ItemLocationBox and
 * more. It acts as a container for descriptive and structural metadata relevant to HEIF-based
 * formats.
 *
 * For technical details, refer to ISO/IEC 14496-12:2015, Page 76 (Meta Box).
 *
 * <p>
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class MetaBox extends FullBox
{
    private static final LogFactory LOGGER = LogFactory.getLogger(MetaBox.class);
    private final Map<HeifBoxType, Box> containedBoxes;

    /**
     * Constructs a {@code MetaBox}, parsing its fields from the specified
     * {@link SequentialByteReader}.
     *
     * @param box
     *        the parent {@link Box} object containing size and type information
     * @param reader
     *        the byte reader for parsing box data
     *
     * @throws IllegalStateException
     *         if malformed data is encountered, such as a negative box size and corrupted data
     */
    public MetaBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        long startpos = reader.getCurrentPosition();
        long endpos = startpos + available();

        containedBoxes = new LinkedHashMap<>();

        do
        {
            String boxType = BoxFactory.peekBoxType(reader);

            /*
             * Just in case, handle unknown boxes to avoid unnecessary object creation.
             */
            if (HeifBoxType.fromTypeName(boxType) == HeifBoxType.UNKNOWN)
            {
                Box unknownBox = new Box(reader);
                reader.skip(unknownBox.available()); // Skip unknown property safely
            }

            else
            {
                Box containedBox = BoxFactory.createBox(reader);
                containedBoxes.put(containedBox.getHeifType(), containedBox);
            }

        } while (reader.getCurrentPosition() < endpos);

        if (reader.getCurrentPosition() != endpos)
        {
            throw new IllegalStateException("Mismatch in expected box size for [" + getTypeAsString() + "]");
        }

        byteUsed += reader.getCurrentPosition() - startpos;
    }

    /**
     * Returns a combined list of all boxes contained in this {@code MetaBox}.
     *
     * @return a list of Box objects in reading order
     */
    @Override
    public List<Box> getBoxList()
    {
        List<Box> combinedList = new ArrayList<>(containedBoxes.values());

        return combinedList;
    }

    /**
     * Logs a single diagnostic line for this box at the debug level.
     *
     * <p>
     * This is useful when traversing the box tree of a HEIF/ISO-BMFF structure for debugging or
     * inspection purposes.
     * </p>
     */
    @Override
    public void logBoxInfo()
    {
        String tab = Box.repeatPrint("\t", getHierarchyDepth());
        LOGGER.debug(String.format("%s%s '%s':\t(%s)", tab, this.getClass().getSimpleName(), getTypeAsString(), getHeifType().getBoxCategory()));
    }
}