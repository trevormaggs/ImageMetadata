package tif;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import common.ByteValueConverter;
import common.DateParser;
import common.Directory;
import common.RationalNumber;
import common.TagValueConverter;
import tif.DirectoryIFD.EntryIFD;
import tif.TagEntries.Taggable;

/**
 * Represents and manages an Image File Directory (IFD) in a TIFF file, conforming to the TIFF 6.0
 * specification. An IFD serves as a container for image-related data and metadata, composed of a
 * series of tag entries that store various attributes.
 *
 * <p>
 * Each IFD can represent an image or a metadata structure associated with an image. Although not
 * all directories contain pixel data, every image is described by at least one IFD.
 * </p>
 *
 * <p>
 * This implementation focuses on basic access to EXIF and standard IFD metadata entries, providing
 * convenient methods for retrieving values in common data formats.
 * </p>
 *
 * <p>
 * <strong>Change History</strong>
 * </p>
 *
 * <ul>
 * <li>v1.0 — Initial release by Trevor Maggs, 10 July 2025</li>
 * </ul>
 *
 * @version 1.0
 * @since 10 July 2025
 * @author Trevor Maggs
 * @see EntryIFD
 */
public class DirectoryIFD implements Directory<EntryIFD>
{
    private DirectoryIdentifier directoryType;
    private ByteOrder headerByteOrder;
    private Map<Integer, EntryIFD> entryMap;

    /**
     * Constructs a new directory instance to manage a collection of IFD entries embedded within the
     * TIFF image file.
     *
     * @param dirType
     *        a directory type defined in the {@link DirectoryIdentifier} enumeration class
     * @param order
     *        the byte order, either {@code ByteOrder.BIG_ENDIAN} or {@code ByteOrder.LITTLE_ENDIAN}
     */
    public DirectoryIFD(DirectoryIdentifier dirType, ByteOrder order)
    {
        this.entryMap = new HashMap<>();
        this.directoryType = dirType;
        this.headerByteOrder = order;
    }

    /**
     * Gets the current Image File Directory type.
     *
     * @return an enumeration value of DirectoryIdentifier
     */
    public DirectoryIdentifier getDirectoryType()
    {
        return directoryType;
    }

    /**
     * Adds a new field to the collection of IFD entries identified within the TIFF image file.
     *
     * @param tag
     *        the tag descriptor
     * @param ttype
     *        the TIFF field type
     * @param length
     *        the number of values
     * @param offset
     *        the raw offset/value field
     * @param bytes
     *        the value bytes (may be null)
     *
     * @return an object of EntryIFD type
     */
    public boolean addEntry(Taggable tag, TifFieldType ttype, int length, int offset, byte[] bytes)
    {
        return add(new EntryIFD(tag, ttype, length, offset, bytes, headerByteOrder));
    }

    /**
     * Checks whether the specified tag holds a value based on a general numerical representation.
     *
     * @param tag
     *        the specific enumeration tag to search for a match
     *
     * @return boolean true if the value held by the tag is numeric, false otherwise
     * @throws IllegalArgumentException
     *         if the tag is not found in the IFD structure
     */
    public boolean isTagNumeric(Taggable tag)
    {
        EntryIFD entry = findEntryByID(tag.getNumberID());

        if (entry == null || entry.getData() == null)
        {
            throw new IllegalArgumentException("Cannot find tag [" + tag + "] in directory [" + tag.getDirectoryType() + "]");
        }

        return entry.getFieldType().isNumber();
    }

    /**
     * Returns the string value associated with the specified tag entry.
     *
     * @param tag
     *        the enumeration tag to obtain the value
     *
     * @return a string representing the value of the specified tag entry
     */
    public String getString(Taggable tag)
    {
        EntryIFD entry = findEntryByID(tag.getNumberID());

        if (entry == null || entry.getData() == null)
        {
            return "";
        }

        return getStringValue(entry);
    }

    /**
     * Retrieves the string value of the specified EntryIFD object.
     *
     * @param entry
     *        an EntryIFD object to retrieve the value from its field
     *
     * @return a String representation of the value associated with the specified EntryIFD object
     */
    public String getStringValue(EntryIFD entry)
    {
        return TagValueConverter.toStringValue(entry);
    }

    /**
     * Retrieves the value of a tag in numeric form, if available.
     * 
     * This method attempts to return the value of the specified tag as a {@link Number}. If the tag
     * is not present, or the value cannot be interpreted as a numeric type, a default value of
     * {@code 0} is returned.
     *
     * @param tag
     *        the tag for which the numeric value is requested
     * @return a Number instance if the tag exists and has numeric content, otherwise, zero
     */
    public Number getNumericValue(Taggable tag)
    {
        EntryIFD entry = findEntryByID(tag.getNumberID());

        if (entry == null || entry.getData() == null || !entry.getFieldType().isNumber())
        {
            return 0;
        }

        return TagValueConverter.toNumericValue(entry);
    }

    /**
     * Returns the integer value associated with the specified tag enumeration.
     *
     * @param tag
     *        the enumeration tag to retrieve the value
     *
     * @return an Integer representation of the tag's value
     */
    public int getIntValue(Taggable tag)
    {
        return getNumericValue(tag).intValue();
    }

    /**
     * Returns the long value associated with the specified tag enumeration.
     *
     * @param tag
     *        the enumeration tag to retrieve the value
     *
     * @return a Long representation of the tag's value
     */
    public long getLongValue(Taggable tag)
    {
        return getNumericValue(tag).longValue();
    }

    /**
     * Returns the float value associated with the specified tag enumeration.
     *
     * @param tag
     *        the enumeration tag to retrieve the value
     *
     * @return a Float representation of the tag's value
     */
    public float getFloatValue(Taggable tag)
    {
        return getNumericValue(tag).floatValue();
    }

    /**
     * Returns the double value associated with the specified tag enumeration.
     *
     * @param tag
     *        the enumeration tag to retrieve the value
     *
     * @return a Double representation of the tag's value
     */
    public double getDoubleValue(Taggable tag)
    {
        return getNumericValue(tag).doubleValue();
    }

    /**
     * Returns the rational number value associated with the specified tag enumeration.
     *
     * @param tag
     *        the enumeration tag to fetch the value
     *
     * @return a Rational Number representation of the tag's value
     * @throws IllegalArgumentException
     *         if the tag is not found in the IFD structure
     */
    public RationalNumber getRationalValue(Taggable tag)
    {
        EntryIFD entry = findEntryByID(tag.getNumberID());

        if (entry == null || entry.getData() == null)
        {
            throw new IllegalArgumentException("Cannot find tag [" + tag + "] in directory [" + tag.getDirectoryType() + "]");
        }

        Object obj = entry.getData();

        if (obj instanceof RationalNumber)
        {
            return ((RationalNumber) obj);
        }

        return null;
    }

    /**
     * Returns a Date object if the tag is marked as a potential Date entry.
     * 
     * @param tag
     *        the enumeration tag to obtain the value
     *
     * @return a Date object representing the value of the specified tag entry, or null if the tag
     *         is not a Date type
     * @throws IllegalArgumentException
     *         if the tag is not found in the IFD structure
     */
    public Date getDate(Taggable tag)
    {
        if (tag.getHint() == TagHint.HINT_DATE)
        {
            EntryIFD entry = findEntryByID(tag.getNumberID());

            if (entry == null || entry.getData() == null)
            {
                throw new IllegalArgumentException("Cannot find tag [" + tag + "] in directory [" + tag.getDirectoryType() + "]");
            }

            return DateParser.convertToDate(entry.getData().toString());
        }

        return null;
    }

    /**
     * Adds a new {@code EntryIFD} entry to this Directory.
     *
     * @param entry
     *        {@code EntryIFD} object
     *
     * @return true if an existing entry was replaced
     */
    @Override
    public boolean add(EntryIFD entry)
    {
        entryMap.put(entry.getTagID(), entry);

        return true;
    }

    /**
     * Removes an entry from this Directory.
     *
     * @param entry
     *        {@code EntryIFD} object to remove
     *
     * @return true if this collection changed as a result of the call if the tag exists, otherwise
     *         false
     */
    @Override
    public boolean remove(EntryIFD entry)
    {
        return (entryMap.remove(entry.getTagID()) != null);
    }

    /**
     * Indicates whether the specified tag has been set.
     *
     * @param tag
     *        the specific tag to search for a match
     *
     * @return true if the tag exists in this Directory, otherwise false
     */
    @Override
    public boolean contains(Taggable tag)
    {
        return (findEntryByID(tag.getNumberID()) != null);
    }

    /**
     * Finds an IFD entry corresponding to the specified tag ID.
     *
     * @param tagid
     *        the tag ID identifying the entry
     *
     * @return an {@code EntryIFD} entry, or null if no matching entry is found
     */
    @Override
    public EntryIFD findEntryByID(int tagid)
    {
        return entryMap.get(tagid);
    }

    /**
     * Returns the count of IFD entries present in this Directory.
     *
     * @return the total number of elements in the Directory
     */
    @Override
    public int length()
    {
        return entryMap.size();
    }

    /**
     * Retrieves an iterator to navigate through a collection of {@code EntryIFD} objects.
     *
     * @return an Iterator object
     */
    @Override
    public Iterator<EntryIFD> iterator()
    {
        return entryMap.values().iterator();
    }

    /**
     * Generates a formatted string showing current values of every IFD entry in the collection.
     *
     * @return a comprehensive, string-based representation of each IFD entry
     */
    @Override
    public String toString()
    {
        StringBuilder output = new StringBuilder();

        for (EntryIFD entry : entryMap.values())
        {
            String value = getStringValue(entry);

            output.append(String.format("  %-20s %s%n", "[Directory Type]", getDirectoryType().getDescription()));
            output.append(entry);
            output.append(String.format("  %-20s %s%n", "[Value]", (value.isEmpty() ? "[Empty]" : value)));
            output.append(System.lineSeparator());
        }

        return output.toString();
    }

    /**
     * Represents a single Image File Directory (IFD) entry within a TIFF structure.
     *
     * Each {@code EntryIFD} encapsulates metadata such as tag ID, data type, count, raw bytes, and
     * a parsed object representation. It is immutable and self-contained.
     *
     *
     * @author Trevor Maggs
     * @since 21 June 2025
     */
    public final static class EntryIFD
    {
        private final Taggable tagEnum;
        private final TifFieldType fieldType;
        private final int count;
        private final int valueOffset;
        private final byte[] value;
        private final Object parsedData;

        /**
         * Constructs an immutable {@code EntryIFD} instance from raw bytes.
         *
         * @param tag
         *        the tag descriptor
         * @param ttype
         *        the TIFF field type
         * @param length
         *        the number of values
         * @param offset
         *        the raw offset/value field
         * @param bytes
         *        the value bytes (may be null)
         * @param byteOrder
         *        the byte order used to interpret binary values
         */
        public EntryIFD(Taggable tag, TifFieldType ttype, int length, int offset, byte[] bytes, ByteOrder byteOrder)
        {
            this.tagEnum = tag;
            this.fieldType = ttype;
            this.count = length;
            this.valueOffset = offset;
            this.value = (bytes != null ? Arrays.copyOf(bytes, bytes.length) : null);
            this.parsedData = parseData(byteOrder);
        }

        /**
         * Parses the raw byte array into a typed Java object based on the TIFF field type.
         *
         * @param order
         *        the byte order used for parsing
         * @return parsed data as {@code Object}, or {@code null} if parsing fails or input is
         *         absent
         */
        private Object parseData(ByteOrder order)
        {
            if (value == null || value.length == 0)
            {
                return null;
            }

            switch (fieldType)
            {
                case TYPE_BYTE_U:
                case TYPE_BYTE_S:
                    return value[0];

                case TYPE_UNDEFINED:
                    return value;

                case TYPE_ASCII:
                    String str = new String(ByteValueConverter.readFirstNullTerminatedByteArray(value), StandardCharsets.UTF_8);
                    return str.isEmpty() ? new String[0] : str;

                case TYPE_SHORT_U:
                    return ByteValueConverter.toUnsignedShort(value, order);

                case TYPE_LONG_U:
                case TYPE_IFD_POINTER:
                    return ByteValueConverter.toUnsignedInteger(value, order);

                case TYPE_RATIONAL_U:
                    return ByteValueConverter.toRational(value, order, RationalNumber.DataType.UNSIGNED);

                case TYPE_SHORT_S:
                    return ByteValueConverter.toShort(value, order);

                case TYPE_LONG_S:
                    return ByteValueConverter.toInteger(value, order);

                case TYPE_RATIONAL_S:
                    return ByteValueConverter.toRational(value, order, RationalNumber.DataType.SIGNED);

                case TYPE_FLOAT:
                    return ByteValueConverter.toFloat(value, order);

                case TYPE_DOUBLE:
                    return ByteValueConverter.toDouble(value, order);

                default:
                    return null;
            }
        }

        /**
         * @return the tag enum that identifies this entry
         */
        public Taggable getTag()
        {
            return tagEnum;
        }

        /**
         * @return the numeric ID of the tag
         */
        public int getTagID()
        {
            return tagEnum.getNumberID();
        }

        /**
         * @return the TIFF field type for this entry
         */
        public TifFieldType getFieldType()
        {
            return fieldType;
        }

        /**
         * @return the number of values represented by this entry
         */
        public long getCount()
        {
            return count;
        }

        /**
         * @return the offset or immediate value field for this entry
         */
        public int getOffset()
        {
            return valueOffset;
        }

        /**
         * @return a copy of the raw value byte array, or null if not set
         */
        public byte[] getByteArray()
        {
            return (value != null ? Arrays.copyOf(value, value.length) : null);
        }

        /**
         * @return the total byte length of the data, based on field type and count
         */
        public int getByteLength()
        {
            return count * fieldType.getElementLength();
        }

        /**
         * Returns the parsed data object representing this entry’s value.
         *
         * @return the parsed data, or null if no value is available
         */
        public Object getData()
        {
            return parsedData;
        }

        /**
         * @return formatted string describing the entry’s key characteristics
         */
        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("  %-20s 0x%04X (%d)%n", "[Tag ID]", getTagID(), getTagID()));
            sb.append(String.format("  %-20s %s%n", "[Tag Name]", getTag()));
            sb.append(String.format("  %-20s %s%n", "[Field Type]", getFieldType()));
            sb.append(String.format("  %-20s %s%n", "[Count]", getCount()));

            if (getByteLength() > IFDHandler.ENTRY_MAX_VALUE_LENGTH)
            {
                sb.append(String.format("  %-20s 0x%04X%n", "[Jump Offset]", valueOffset));
            }

            return sb.toString();
        }
    }
}