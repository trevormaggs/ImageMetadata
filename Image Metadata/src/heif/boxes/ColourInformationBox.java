package heif.boxes;

import java.nio.charset.StandardCharsets;
import common.SequentialByteReader;
import logger.LogFactory;

/**
 * This derived class handles the Box identified as {@code colr} - Colour information Box. For
 * technical details, refer to the Specification document - ISO/IEC 14496-12:2015 in Page 158.
 *
 * This box contains information about the colour space of the image. Its content varies based on
 * the {@code colourType} field.
 *
 * <p>
 * <strong>API Note:</strong>This implementation handles {@code nclx} colour types fully. For
 * {@code rICC} and {@code prof} types, the ICC profile data is ignored as its parsing is beyond the
 * current scope of this box. Further testing is needed for edge cases and compatibility if
 * required.
 * </p>
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class ColourInformationBox extends Box
{
    private static final LogFactory LOGGER = LogFactory.getLogger(ColourInformationBox.class);
    public static final String TYPE_NCLX = "nclx";
    public static final String TYPE_RICC = "rICC";
    public static final String TYPE_PROF = "prof";
    private final String colourType;
    private int colourPrimaries;
    private int transferCharacteristics;
    private int matrixCoefficients;
    private boolean isFullRangeFlag;
    private byte[] iccProfileData;

    /**
     * Constructs a {@code ColourInformationBox} from a parent Box and a byte reader. This
     * constructor parses the specific fields of the {@code colr} box based on its 'colourType'.
     *
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     * 
     * @throws IllegalStateException
     *         if malformed data is encountered, such as a negative box size and corrupted data
     */
    public ColourInformationBox(Box box, SequentialByteReader reader)
    {
        super(box);

        long startpos = reader.getCurrentPosition();
        long endpos = startpos + available();
        int remainingBytes = available();

        // Read 4-byte colourType
        colourType = new String(reader.readBytes(4), StandardCharsets.US_ASCII);
        remainingBytes -= 4;

        if (colourType.equals("nclx"))
        {
            if (remainingBytes < 7)
            {
                // For nclx specific data, 3 shorts + 1 byte = 7 bytes are required
                LOGGER.warn("Not enough bytes for [nclx] ColourInformationBox. Expected 7, but found [" + remainingBytes + "]. Box may be malformed");

                throw new IllegalStateException("Mismatch in expected box size for [" + getTypeAsString() + "]");
            }

            colourPrimaries = reader.readUnsignedShort();
            transferCharacteristics = reader.readUnsignedShort();
            matrixCoefficients = reader.readUnsignedShort();
            isFullRangeFlag = (((reader.readByte() & 0x80) >> 7) == 1);

            // Just ignore the last 7 bits, which are reserved
            // int reserved = (bits & 0x7F);
        }

        else if (colourType.equals("rICC") || colourType.equals("prof"))
        {
            /*
             * Both restricted ICC profile ('rICC') and unrestricted ICC profile ('prof') are
             * currently not in scope for parsing, therefore we just read them silently and safely.
             */
            if (remainingBytes > 0)
            {
                this.iccProfileData = reader.readBytes(remainingBytes);
                LOGGER.info("Read [" + remainingBytes + "] bytes of ICC profile data for [" + colourType + "] colour type, but it is not used");
            }
        }

        else if (remainingBytes > 0)
        {
            reader.skip(remainingBytes);
            LOGGER.warn("Unknown colourType [" + colourType + "] encountered in ColourInformationBox. Skipping remaining [" + remainingBytes + "] bytes");
        }

        if (reader.getCurrentPosition() != endpos)
        {
            throw new IllegalStateException("Mismatch in expected box size for [" + getTypeAsString() + "]");
        }

        byteUsed += reader.getCurrentPosition() - startpos;
    }

    /**
     * Returns the 4-character string identifying the colour type. Examples include {@code nclx},
     * {@code rICC} and {@code prof}.
     *
     * @return the colour type string
     */
    public String getColourType()
    {
        return colourType;
    }

    /**
     * Returns the colour primaries value for {@code nclx} colour types. This value defines the
     * chromaticity of the primaries and the white point.
     *
     * @return the colour primaries, or 0 if not an 'nclx' type or not parsed
     */
    public int getColourPrimaries()
    {
        return colourPrimaries;
    }

    /**
     * Returns the transfer characteristics value for {@code nclx} colour types. This value defines
     * the opto-electronic transfer characteristic of the source picture.
     *
     * @return the transfer characteristics, or 0 if not an 'nclx' type or not parsed.
     */
    public int getTransferCharacteristics()
    {
        return transferCharacteristics;
    }

    /**
     * Returns the matrix coefficients value for {@code nclx} colour types. This value defines the
     * matrix coefficients used in deriving luminance and chrominance signals.
     *
     * @return the matrix coefficients, or 0 if not an 'nclx' type or not parsed
     */
    public int getMatrixCoefficients()
    {
        return matrixCoefficients;
    }

    /**
     * Returns the full range flag for {@code nclx} colour types. {@code true} indicates full range
     * representation, {@code false} indicates limited range.
     *
     * @return the full range flag, or {@code false} if not an 'nclx' type or not parsed
     */
    public boolean isFullRangeFlag()
    {
        return isFullRangeFlag;
    }

    /**
     * Returns the full bytes of ICC profile data as an array.
     *
     * @return the array of bytes
     */
    public byte[] getIccProfile()
    {
        return (iccProfileData != null) ? iccProfileData.clone() : new byte[0];
    }

    /**
     * Returns a string representation of this {@code ColourInformationBox}.
     *
     * @return a formatted string describing the box contents.
     */
    @Override
    public String toString()
    {
        return toString(null);
    }

    /**
     * Returns a human-readable debug string, summarising the colour information contained in this
     * {@code ColourInformationBox}. Useful for logging or diagnostics.
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

        sb.append(String.format("%s '%s': Type=%s", this.getClass().getSimpleName(), getTypeAsString(), colourType));

        if (TYPE_NCLX.equals(colourType))
        {
            sb.append(String.format(", Primaries=0x%04X, Transfer=0x%04X, Matrix=0x%04X, FullRange=%b", colourPrimaries, transferCharacteristics, matrixCoefficients, isFullRangeFlag));
        }

        else if (TYPE_RICC.equals(colourType) || TYPE_PROF.equals(colourType))
        {
            sb.append(" (ICC Profile data skipped)");
        }

        else
        {
            sb.append(" (Unknown colour type)");
        }

        sb.append(System.lineSeparator());

        return sb.toString();
    }
}