package heif.boxes;

import common.SequentialByteReader;
import logger.LogFactory;

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
    private static final LogFactory LOGGER = LogFactory.getLogger(ItemDataBox.class);
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
        StringBuilder sb = new StringBuilder();
        String tab = Box.repeatPrint("\t", getHierarchyDepth());
        LOGGER.debug(String.format("%s%s '%s':", tab, this.getClass().getSimpleName(), getTypeAsString()));

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

        LOGGER.debug(sb.toString());
    }
}