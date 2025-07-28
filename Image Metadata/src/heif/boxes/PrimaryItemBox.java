package heif.boxes;

import common.SequentialByteReader;

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
 * <h3>Version History:</h3>
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 31 May 2025</li>
 * </ul>
 * 
 * <p>
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 *
 * @author Trevor Maggs
 * @since 31 May 2025
 */
public class PrimaryItemBox extends FullBox
{
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

        int pos = reader.getCurrentPosition();

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
     * Returns a string representation of this {@code PrimaryItemBox}.
     *
     * @return a formatted string describing the box contents.
     */
    @Override
    public String toString()
    {
        return toString(null);
    }

    /**
     * Returns a human-readable debug string, summarising this {@code PrimaryItemBox}.
     *
     * @param prefix
     *        an optional label or heading to prepend. Can be {@code null}
     * 
     * @return a formatted string suitable for logging or inspection.
     */
    @Override
    public String toString(String prefix)
    {
        StringBuilder sb = new StringBuilder();

        if (prefix != null && !prefix.isEmpty())
        {
            sb.append(prefix);
        }

        for (int i = 0; i < getHierarchyDepth(); i++)
        {
            sb.append("\t");
        }

        sb.append(String.format("%s '%s':\t\tPrimaryItemID=%d", this.getClass().getSimpleName(), getTypeAsString(), getItemID()));
        sb.append(System.lineSeparator());

        return sb.toString();
    }
}