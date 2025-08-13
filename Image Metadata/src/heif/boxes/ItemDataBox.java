package heif.boxes;

import common.SequentialByteReader;

/**
 * This derived Box class handles the Box identified as {@code idat} - Item Data Box. For technical
 * details, refer to the Specification document - {@code ISO/IEC 14496-12:2015} on Page 86.
 *
 * This box contains the data of metadata items that use the construction method indicating that an
 * item’s data extents are stored within this box.
 * 
 * <p>
 * <strong>API Note:</strong> This implementation assumes a flat byte array. No item parsing is
 * performed beyond raw byte extraction. Further testing is needed for edge cases and compatibility.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class ItemDataBox extends Box
{
    private final byte[] data;

    /**
     * This constructor creates a derived Box object, providing additional data of metadata items.
     * 
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public ItemDataBox(Box box, SequentialByteReader reader)
    {
        super(box);

        // Number of bytes remaining for this box payload
        int count = available();
        long pos = reader.getCurrentPosition();

        data = reader.readBytes(count);
        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns a copy of the raw data stored in this {@code ItemDataBox} resource.
     *
     * @return the item data as a byte array
     */
    public byte[] getData()
    {
        return data.clone();
    }

    /**
     * Returns a string representation of this {@code ItemDataBox} resource.
     *
     * @return a formatted string describing the box contents.
     */
    @Override
    public String toString()
    {
        return toString(null);
    }

    /**
     * Returns a human-readable debug string, summarising the raw data stored in this
     * {@code ItemDataBox}. Useful for logging or diagnostics.
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

        for (int i = 0; i < getHierarchyDepth(); i++)
        {
            sb.append("\t");
        }

        sb.append(String.format("%s '%s':", this.getClass().getSimpleName(), getTypeAsString()));
        sb.append(System.lineSeparator());

        if (data.length < 65)
        {
            sb.append(String.format("\t\tData bytes: "));

            for (byte b : data)
            {
                sb.append(String.format("0x%02X ", b));
            }
        }

        else
        {
            sb.append(String.format("\t\tData size: %d bytes (hex dump omitted)", data.length));
        }

        sb.append(System.lineSeparator());

        return sb.toString();
    }
}