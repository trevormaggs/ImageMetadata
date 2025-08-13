package tif;

/**
 * An enumeration class of data format types defined in the TIFF specification 6.0 document.
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public enum TifFieldType
{
    TYPE_ERROR(0, "Unknown type. Error", 0),
    TYPE_BYTE_U(1, "Flag for 8-bit unsigned integer", 1),
    TYPE_ASCII(2, "Flag for null terminated ASCII string", 1),
    TYPE_SHORT_U(3, "Flag for 16-bit unsigned integer (2 bytes)", 2),
    TYPE_LONG_U(4, "Flag for 32-bit unsigned integer (4 bytes)", 4),
    TYPE_RATIONAL_U(5, "Flag for pairs of unsigned integers", 8),
    TYPE_BYTE_S(6, "Flag for 8-bit signed integer", 1),
    TYPE_UNDEFINED(7, "Flag for 8 bit uninterpreted byte", 1),
    TYPE_SHORT_S(8, "Flag for 16-bit signed integer (2 bytes)", 2),
    TYPE_LONG_S(9, "Flag for 32-bit signed integer (4 bytes)", 4),
    TYPE_RATIONAL_S(10, "Flag for pairs of signed integers", 8),
    TYPE_FLOAT(11, "Flag for single precision float (4 bytes)", 4),
    TYPE_DOUBLE(12, "Flag for double precision double (8 bytes)", 8),
    TYPE_IFD_POINTER(13, "Flag for IFD pointer defined in TIFF Tech Note 1 in TIFF Specification Supplement 1", 4);

    /*
     * Need took at expanding to include these new types?
     * 
     * TIFF_LONG8 = 16;
     * TIFF_SLONG8 = 17;
     * TIFF_IFD8 = 18;
     * TIFF_LAZY_LONG = 14;
     * TIFF_LAZY_LONG8 = 15;
     */

    /**
     * The numerically smallest and largest values representing a TIFF data type.
     */
    public static final int MIN_DATATYPE = TYPE_BYTE_U.getDataType();
    public static final int MAX_DATATYPE = TYPE_IFD_POINTER.getDataType();

    private final int dataType;
    private final String description;
    private final int elementLength;

    /**
     * Constructs an instance of this enumeration class to represent a specific data format type.
     *
     * @param fmt
     *        the data format type
     * @param desc
     *        the description to indicate the data type
     * @param len
     *        the required length of bytes
     */
    private TifFieldType(int fmt, String desc, int len)
    {
        dataType = fmt;
        description = desc;
        elementLength = len;
    }

    /**
     * Retrieves the data format type associated with this field.
     *
     * @return the data format type as an integer
     */
    public int getDataType()
    {
        return dataType;
    }

    /**
     * Retrieves the description.
     *
     * @return the description as a string
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Retrieves the length associated with this field.
     *
     * @return the length as an integer
     */
    public int getElementLength()
    {
        return elementLength;
    }

    /**
     * Searches through the enumeration to find the TifFieldType constant that matches the specified
     * data type.
     *
     * @param typeCode
     *        the data type to search for
     * 
     * @return the TifFieldType constant representing the data type
     */
    public static TifFieldType getTiffType(int typeCode)
    {
        for (TifFieldType type : values())
        {
            if (type.dataType == typeCode)
            {
                return type;
            }
        }

        return TYPE_ERROR;
    }

    /**
     * Verifies if the given data type can be used for the data associated with this tag.
     *
     * @param dataType
     *        the data type to be checked, such as {@code TYPE_BYTE_U}, {@code TYPE_SHORT_S}, etc
     * 
     * @return a boolean indicating whether the given data type can be used with this tag
     * 
     * @throws IllegalArgumentException
     *         if the data type is less than {@code MIN_DATATYPE} or greater than
     *         {@code MAX_DATATYPE}
     */
    public static boolean dataTypeinRange(int dataType)
    {
        if (dataType < MIN_DATATYPE || dataType > MAX_DATATYPE)
        {
            throw new IllegalArgumentException("Data format type not in range. Illegal value detected.");
        }

        return (1 << dataType > 0);
    }

    /**
     * Verifies that this tag points to an IFD structure containing additional tags.
     *
     * @return boolean {@code true} if this tag indicates an IFD structure
     */
    public boolean isIFDPointer()
    {
        return (this == TYPE_IFD_POINTER);
    }

    /**
     * Verifies that this tag contains a numeric value.
     *
     * @return boolean {@code true} if the value is numeric
     */
    public boolean isNumber()
    {
        switch (this)
        {
            case TYPE_SHORT_U:
            case TYPE_LONG_U:
            case TYPE_SHORT_S:
            case TYPE_LONG_S:
            case TYPE_FLOAT:
            case TYPE_DOUBLE:
                return true;

            default:
                return false;
        }
    }

    /**
     * Verifies that this tag contains a string value.
     *
     * @return boolean {@code true} if the value is a string
     */
    public boolean isString()
    {
        return (this == TYPE_ASCII);
    }

    /**
     * Confirms that this tag contains a Rational Number class object.
     *
     * @return boolean {@code true} if the value is stored in a Rational Number object
     */
    public boolean isRationalNumber()
    {
        return (this == TYPE_RATIONAL_U || this == TYPE_RATIONAL_S);
    }

    /**
     * Confirms that this tag contains a byte value.
     *
     * @return boolean {@code true} if the value is a byte
     */
    public boolean isByteData()
    {
        return (this == TYPE_BYTE_U || this == TYPE_BYTE_S);
    }
}