package heif.boxes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import common.SequentialByteReader;
import heif.BoxFactory;
import heif.HeifBoxType;

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
 * Version History:
 * </p>
 *
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 22 July 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 22 July 2025
 * @implNote Additional testing is required to confirm the reliability and robustness of this
 *           implementation.
 */
public class MetaBox extends FullBox
{
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

        containedBoxes = new LinkedHashMap<>();

        int startpos = reader.getCurrentPosition();
        int endpos = startpos + available();

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
                // System.out.printf("LOOK %s\n", containedBox.getTypeAsString());
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
     * Returns a string representation of this {@code FileTypeBox} resource.
     *
     * @return a formatted string describing the box contents
     */
    @Override
    public String toString()
    {
        return toString(null);
    }

    /**
     * Returns a human-readable debug string, summarising structured references associated with this
     * HEIF-based file. Useful for logging or diagnostics.
     *
     * @param prefix
     *        Optional heading or label to prepend. Can be null
     * 
     * @return a formatted string suitable for debugging, inspection, or textual analysis
     */
    @Override
    public String toString(String prefix)
    {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isEmpty())
        {
            sb.append(prefix);
        }

        sb.append(String.format("%s '%s':\t(%s)%n", this.getClass().getSimpleName(), getTypeAsString(), getHeifType().getBoxCategory()));

        for (Box box : containedBoxes.values())
        {
            sb.append(box.toString("\t"));
        }

        return sb.toString();
    }
}