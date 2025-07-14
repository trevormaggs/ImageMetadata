package heif.boxes;

import java.util.ArrayList;
import java.util.List;
import common.SequentialByteReader;

/**
 * The {@code ItemLocationBox} class handles the HEIF Box identified as {@code iloc} (Item Location
 * Box).
 * 
 * <p>
 * This box provides a directory of item resources, either in the same file or in external files.
 * Each entry describes the item's container, offset within that container, and length.
 * </p>
 * 
 * <p>
 * For technical details, refer to the specification document: {@code ISO/IEC 14496-12:2015}, pages
 * 77–80.
 * </p>
 * 
 * <p>
 * Version History:
 * </p>
 * 
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 2 June 2025</li>
 * </ul>
 * 
 * @author Trevor Maggs
 * @since 2 June 2025
 * @implNote This is not a completed implementation. Rigorous testing is required to validate its
 *           reliability and robustness
 */
public class ItemLocationBox extends FullBox
{
    private int offsetSize;
    private int lengthSize;
    private int baseOffsetSize;
    private int indexSize;
    private int itemCount;
    private List<ExtentData> extentList;

    /**
     * Constructs an {@code ItemLocationBox} by parsing the provided {@code iloc} box data.
     *
     * @param box
     *        the parent {@code Box} containing common box values
     * @param reader
     *        a {@code SequentialByteReader} for sequential access to the box content
     */
    public ItemLocationBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int tmp;
        int extentCount;
        int pos = reader.getCurrentPosition();

        extentList = new ArrayList<>();

        tmp = reader.readUnsignedByte();

        offsetSize = (tmp & 0xF0) >> 4;
        lengthSize = (tmp & 0x0F);

        tmp = reader.readUnsignedByte();

        baseOffsetSize = (tmp & 0xF0) >> 4;

        if (isVersion1or2())
        {
            indexSize = (tmp & 0x0F);
        }

        itemCount = (getVersion() < 2 ? reader.readUnsignedShort() : (getVersion() == 2 ? (int) reader.readUnsignedInteger() : 0));

        for (int i = 0; i < itemCount; i++)
        {
            int extentIndex = 0;
            int constructionMethod = 0;
            int dataReferenceIndex;
            long baseOffset;
            long extentOffset;
            int extentLength;
            final int itemID = (getVersion() < 2) ? reader.readUnsignedShort() : (int) reader.readUnsignedInteger();

            if (isVersion1or2())
            {
                constructionMethod = reader.readUnsignedShort() & 0x000F;
            }

            dataReferenceIndex = reader.readUnsignedShort();
            baseOffset = (baseOffsetSize == 8 ? reader.readLong() : (baseOffsetSize == 4 ? reader.readUnsignedInteger() : 0));
            extentCount = reader.readUnsignedShort();

            for (int j = 0; j < extentCount; j++)
            {
                if (isVersion1or2() && indexSize > 0)
                {
                    extentIndex = (int) readSizedValue(indexSize, reader);
                }

                extentOffset = readSizedValue(offsetSize, reader);
                extentLength = (int) readSizedValue(lengthSize, reader);

                extentList.add(new ExtentData(itemID, extentCount, extentIndex, extentOffset + baseOffset, extentLength, constructionMethod, dataReferenceIndex));
            }
        }

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Reads a value from the stream based on the specified size indicator.
     * 
     * <p>
     * Allowed input values are:
     * </p>
     * 
     * <ul>
     * <li>{@code 0} – value is always zero (no bytes read)</li>
     * <li>{@code 4} – reads a 4-byte unsigned integer</li>
     * <li>{@code 8} – reads an 8-byte unsigned integer</li>
     * </ul>
     * 
     * <p>
     * For details, refer to ISO/IEC 14496-12:2015, page 77.
     * </p>
     * 
     * @param input
     *        the number of bytes to read: {0, 4, 8}
     * @param reader
     *        a {@code SequentialByteReader} for reading the value
     * 
     * @return the parsed value as an unsigned {@code long}
     * 
     * @throws IllegalArgumentException
     *         if {@code input} is not one of {0, 4, 8}
     */
    private long readSizedValue(int input, SequentialByteReader reader)
    {
        long value;

        switch (input)
        {
            case 0:
                value = 0L;
            break;
            case 4:
                value = reader.readUnsignedInteger();
            break;
            case 8:
                value = reader.readLong();
            break;
            default:
                throw new IllegalArgumentException("The value is out of bounds. It must be {0, 4, 8}. Actual: [" + input + "]");
        }

        return value;
    }

    /**
     * Checks whether this box uses version 1 or version 2 of the {@code ItemLocationBox} format.
     *
     * @return true if the box version is 1 or 2, otherwise false
     */
    private boolean isVersion1or2()
    {
        return getVersion() == 1 || getVersion() == 2;
    }

    /**
     * Finds the first extent matching the specified {@code itemID}.
     *
     * @param itemID
     *        the item identifier to search for
     * 
     * @return the matching ExtentData resource, or null if not found
     */
    public ExtentData findFirstExtentData(int itemID)
    {
        for (ExtentData extent : extentList)
        {
            if (extent.getItemID() == itemID)
            {
                return extent;
            }
        }

        return null;
    }

    /**
     * Finds all extents in this box that match the given {@code itemID}.
     *
     * @param itemID
     *        the item identifier to search for
     * 
     * @return a new list of matching ExtentData, or an empty list if none found
     */
    public List<ExtentData> findExtentDataList(int itemID)
    {
        List<ExtentData> matchingExtents = new ArrayList<>();

        for (ExtentData extent : extentList)
        {
            if (extent.getItemID() == itemID)
            {
                matchingExtents.add(extent);
            }
        }

        return matchingExtents;
    }

    /**
     * Returns a structured summary of this {@code ItemLocationBox}, including item references,
     * extent counts, offsets, and lengths. Useful for analytical or debugging purposes.
     *
     * @return a formatted string representing the box structure
     */
    @Override
    public String showBoxStructure()
    {
        StringBuilder line = new StringBuilder();

        line.append(String.format("\t%s '%s':\titemCount=%d", this.getClass().getSimpleName(), getBoxName(), itemCount));
        line.append(System.lineSeparator());

        for (ExtentData extent : extentList)
        {
            line.append(String.format("\t\titem_ID=%d,\textent_count=%d,\textent_offset=0x%04X,\textent_length=%d%n", extent.getItemID(), extent.extentCount, extent.getExtentOffset(), extent.getExtentLength()));
        }

        return line.toString();
    }

    /**
     * Generates a string representation of the derived Box structure.
     *
     * @return a formatted string
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(super.toString());
        line.append(String.format("  %-24s %s%n", "[Offset Size]", offsetSize));
        line.append(String.format("  %-24s %s%n", "[Length Size]", lengthSize));
        line.append(String.format("  %-24s %s%n", "[Base Offset Size]", baseOffsetSize));
        line.append(String.format("  %-24s %s%n", "[Index Size]", indexSize));
        line.append(String.format("  %-24s %s%n", "[Item Count]", itemCount));

        for (ExtentData list : extentList)
        {
            line.append(System.lineSeparator());
            line.append(String.format("  \t%-24s %s%n", "[Item ID]", list.itemID));
            line.append(String.format("  \t%-24s %s%n", "[Extent Count]", list.extentCount));
            line.append(String.format("  \t%-24s %s%n", "[Extent Index]", list.extentIndex));
            line.append(String.format("  \t%-24s 0x%016X%n", "[Offset]", list.offset));
            line.append(String.format("  \t%-24s %s%n", "[Length]", list.length));
            line.append(String.format("  \t%-24s %s%n", "[Construction Method]", list.constructionMethod));
            line.append(String.format("  \t%-24s %s%n", "[Data Reference Index]", list.dataReferenceIndex));

        }

        return line.toString();
    }

    /**
     * Represents a single extent entry in the {@code ItemLocationBox}.
     *
     * <p>
     * Each {@code ExtentData} instance describes the location of a resource, such as image
     * properties, thumbnails, or Exif metadata. It records the offset, length, construction method,
     * and reference information.
     * </p>
     */
    public static class ExtentData
    {
        private final int itemID;
        private final int extentCount;
        private final int extentIndex;
        private final long offset;
        private final int length;
        private final int constructionMethod;
        private final int dataReferenceIndex;

        private ExtentData(int itemID, int extentCount, int extentIndex, long offset, int length, int construct, int dref)
        {
            this.itemID = itemID;
            this.extentCount = extentCount;
            this.extentIndex = extentIndex;
            this.offset = offset;
            this.length = length;
            this.constructionMethod = construct;
            this.dataReferenceIndex = dref;
        }

        public int getItemID()
        {
            return itemID;
        }

        public long getExtentOffset()
        {
            return offset;
        }

        public int getExtentLength()
        {
            return length;
        }

        /**
         * Returns a formatted string representing this extent's fields and values.
         *
         * @return a detailed string representation of this {@code ExtentData}
         */
        @Override
        public String toString()
        {
            StringBuilder line = new StringBuilder();

            line.append(String.format("  \t%-20s %s%n", "[Item ID]", getItemID()));
            line.append(String.format("  \t%-20s %s%n", "[Extent Count]", extentCount));
            line.append(String.format("  \t%-20s %s%n", "[Extent Index]", extentIndex));
            line.append(String.format("  \t%-20s 0x%016X%n", "[Offset]", getExtentOffset()));
            line.append(String.format("  \t%-20s %s%n", "[Length]", getExtentLength()));
            line.append(String.format("  \t%-20s %s%n", "[Construction Method]", constructionMethod));
            line.append(String.format("  \t%-20s %s%n", "[Data Reference Index]", dataReferenceIndex));

            return line.toString();
        }
    }
}