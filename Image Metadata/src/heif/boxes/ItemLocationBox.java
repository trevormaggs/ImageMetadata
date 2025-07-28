package heif.boxes;

import java.util.ArrayList;
import java.util.Collections;
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
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 2 June 2025</li>
 * </ul>
 * 
 * <p>
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 * 
 * @author Trevor Maggs
 * @since 2 June 2025
 */
public class ItemLocationBox extends FullBox
{
    private final int offsetSize;
    private final int lengthSize;
    private final int baseOffsetSize;
    private final int indexSize;
    private final int itemCount;
    private final List<ItemLocationEntry> items;

    /**
     * Constructs an {@code ItemLocationBox} by parsing the provided {@code iloc} box data.
     *
     * @param box
     *        the parent {@code Box} containing common box values
     * @param reader
     *        a {@code SequentialByteReader} for sequential access to the box content
     * 
     * @throws UnsupportedOperationException
     *         if external data references (dataReferenceIndex != 0) are found
     */
    public ItemLocationBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        int tmp = reader.readUnsignedByte();
        offsetSize = (tmp & 0xF0) >> 4;
        lengthSize = (tmp & 0x0F);

        tmp = reader.readUnsignedByte();
        baseOffsetSize = (tmp & 0xF0) >> 4;

        if (isVersion1or2())
        {
            indexSize = (tmp & 0x0F);
        }
        else
        {
            indexSize = 0;
        }

        itemCount = (getVersion() < 2) ? reader.readUnsignedShort() : (getVersion() == 2 ? (int) reader.readUnsignedInteger() : 0);

        items = new ArrayList<>();

        for (int i = 0; i < itemCount; i++)
        {
            final int itemID = (getVersion() < 2) ? reader.readUnsignedShort() : (int) reader.readUnsignedInteger();

            int constructionMethod = 0;

            if (isVersion1or2())
            {
                constructionMethod = reader.readUnsignedShort() & 0x000F;
            }

            int dataReferenceIndex = reader.readUnsignedShort();

            if (dataReferenceIndex != 0)
            {
                throw new UnsupportedOperationException("External data reference index [" + dataReferenceIndex + "] not supported");
            }

            long baseOffset = readSizedValue(baseOffsetSize, reader);
            int extentCount = reader.readUnsignedShort();
            List<ExtentData> extents = new ArrayList<>(extentCount);

            for (int j = 0; j < extentCount; j++)
            {
                int extentIndex = 0;

                if (isVersion1or2() && indexSize > 0)
                {
                    extentIndex = (int) readSizedValue(indexSize, reader);
                }

                long extentOffset = readSizedValue(offsetSize, reader);
                int extentLength = (int) readSizedValue(lengthSize, reader);

                extents.add(new ExtentData(itemID, extentIndex, extentOffset + baseOffset, extentLength));
            }

            items.add(new ItemLocationEntry(itemID, constructionMethod, dataReferenceIndex, baseOffset, extents));
        }

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Finds the item with the specified {@code itemID}.
     *
     * @param itemID
     *        the item identifier to search for
     * 
     * @return the matching {@code ItemLocationEntry}, or {@code null} if not found
     */
    public ItemLocationEntry findItem(int itemID)
    {
        for (ItemLocationEntry item : items)
        {
            if (item.getItemID() == itemID)
            {
                return item;
            }
        }

        return null;
    }

    /**
     * Finds all extents associated with the specified {@code itemID}.
     *
     * @param itemID
     *        the item identifier to search for
     * 
     * @return an unmodifiable list of extents for the item, or empty list if none found
     */
    public List<ExtentData> findExtentsForItem(int itemID)
    {
        ItemLocationEntry item = findItem(itemID);

        return item == null ? Collections.emptyList() : Collections.unmodifiableList(item.getExtents());
    }

    /**
     * Returns the list of all items.
     *
     * @return an unmodifiable list of items
     */
    public List<ItemLocationEntry> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    /**
     * Returns a string representation of this {@code ItemLocationBox} resource.
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

        for (int i = 0; i < getHierarchyDepth(); i++)
        {
            sb.append("\t");
        }

        sb.append(String.format("%s '%s':\titemCount=%d%n", this.getClass().getSimpleName(), getTypeAsString(), itemCount));

        for (ItemLocationEntry item : items)
        {
            sb.append(String.format("\t\tItemID=%-10d constructionMethod=%-5d dataRefIdx=%-8d baseOffset=0x%X%n", item.getItemID(), item.getConstructionMethod(), item.getDataReferenceIndex(), item.getBaseOffset()));

            for (ExtentData extent : item.getExtents())
            {
                sb.append(String.format("\t\textentIndex=%-5d extentOffset=0x%08X  extentLength=%d%n%n", extent.getExtentIndex(), extent.getExtentOffset(), extent.getExtentLength()));
            }
        }

        return sb.toString();
    }

    /**
     * Reads a value from the stream based on the specified size indicator.
     * 
     * <ul>
     * <li>{@code 0} – value is always zero (no bytes read)</li>
     * <li>{@code 4} – reads a 4-byte unsigned integer</li>
     * <li>{@code 8} – reads an 8-byte unsigned integer</li>
     * </ul>
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
        switch (input)
        {
            case 0:
                return 0L;
            case 4:
                return reader.readUnsignedInteger();
            case 8:
                return reader.readLong();
            default:
                throw new IllegalArgumentException("Invalid input size: " + input);
        }
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
     * Represents one item entry, holding multiple extents.
     */
    public static class ItemLocationEntry
    {
        private final int itemID;
        private final int constructionMethod;
        private final int dataReferenceIndex;
        private final long baseOffset;
        private final List<ExtentData> extents;

        public ItemLocationEntry(int itemID, int constructionMethod, int dataReferenceIndex, long baseOffset, List<ExtentData> extents)
        {
            this.itemID = itemID;
            this.constructionMethod = constructionMethod;
            this.dataReferenceIndex = dataReferenceIndex;
            this.baseOffset = baseOffset;
            this.extents = extents;
        }

        public int getItemID()
        {
            return itemID;
        }

        public int getConstructionMethod()
        {
            return constructionMethod;
        }

        public int getDataReferenceIndex()
        {
            return dataReferenceIndex;
        }

        public long getBaseOffset()
        {
            return baseOffset;
        }

        public List<ExtentData> getExtents()
        {
            return extents;
        }
    }

    /**
     * Represents a single extent entry in the {@code ItemLocationBox}.
     */
    public static class ExtentData
    {
        private final int itemID;
        private final int extentIndex;
        private final long extentOffset;
        private final int extentLength;

        public ExtentData(int itemID, int extentIndex, long extentOffset, int extentLength)
        {
            this.itemID = itemID;
            this.extentIndex = extentIndex;
            this.extentOffset = extentOffset;
            this.extentLength = extentLength;
        }

        public int getItemID()
        {
            return itemID;
        }

        public int getExtentIndex()
        {
            return extentIndex;
        }

        public long getExtentOffset()
        {
            return extentOffset;
        }

        public int getExtentLength()
        {
            return extentLength;
        }
    }
}