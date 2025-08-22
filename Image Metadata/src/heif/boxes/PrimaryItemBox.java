package heif.boxes;

import common.SequentialByteReader;
import logger.LogFactory;

/**
 * Represents the {@code pitm} (Primary Item Box) in HEIF/ISOBMFF files.
 *
 * <p>
 * The Primary Item Box designates a specific item as the "primary" item for a given context.
 * Typically, this is the main image or media item. The actual data may be stored elsewhere in the
 * file or referenced via other boxes, for example, {@code iref}, {@code iloc}.
 * </p>
 *
 * <p>
 * Normally, the content of this box takes either 2 or 4 bytes depending on its version. (When
 * considering the box header, the total size is typically 14 or 16 bytes, depending on field
 * lengths.)
 * </p>
 *
 * <p>
 * Specification Reference: ISO/IEC 14496-12:2015, Section 8.11.4 (Page 80).
 * </p>
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
public class PrimaryItemBox extends FullBox
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PrimaryItemBox.class);
    private final long itemID;

    /**
     * Constructs a {@code PrimaryItemBox}, reading its fields from the specified
     * {@link SequentialByteReader} parameter.
     *
     * @param box
     *        the parent {@link Box} containing size and type information
     * @param reader
     *        The reader for sequential byte parsing
     */
    public PrimaryItemBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        long pos = reader.getCurrentPosition();

        itemID = (getVersion() == 0) ? reader.readUnsignedShort() : reader.readUnsignedInteger();
        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns the identifier of the primary item.
     *
     * @return the primary item ID
     */
    public long getItemID()
    {
        return itemID;
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
        LOGGER.debug(String.format("%s%s '%s':\t\tPrimaryItemID=%d", tab, this.getClass().getSimpleName(), getTypeAsString(), getItemID()));
    }
}