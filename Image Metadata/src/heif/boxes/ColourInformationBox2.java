package heif.boxes;

import java.nio.charset.StandardCharsets;
import common.SequentialByteReader;
import logger.LogFactory; // Assuming LogFactory is available

/**
 * This derived class handles the Box identified as {@code colr} - Colour information Box. For
 * technical details, refer to the Specification document - ISO/IEC 14496-12:2015 in Page 158.
 *
 * This box contains information about the color space of the image. Its content varies based on the
 * {@code colourType} field.
 *
 * <p>
 * Version History:
 * </p>
 *
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 28 May 2025</li>
 * <li>1.1 - Added getters for parsed colour properties and enhanced toString() by Trevor Maggs on
 * 24 July 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 28 May 2025
 * @implNote This implementation handles 'nclx' colour types fully. For 'rICC' and 'prof' types,
 *           the ICC profile data is skipped as its parsing is beyond the current scope of this box.
 *           Further testing is needed for edge cases and compatibility.
 */
public class ColourInformationBox2 extends Box
{

    private static final LogFactory LOGGER = LogFactory.getLogger(ColourInformationBox2.class);

    private final String colourType;
    private int colourPrimaries;
    private int transferCharacteristics;
    private int matrixCoefficients;
    private boolean isFullRangeFlag;
    private byte[] iccProfileData;

    /**
     * Constructs a {@code ColourInformationBox} from a parent Box and a byte reader.
     * This constructor parses the specific fields of the 'colr' box based on its `colourType`.
     *
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     * 
     * @throws IllegalStateException
     *         if malformed data is encountered, such as a negative box size and corrupted data
     */
    public ColourInformationBox2(Box box, SequentialByteReader reader)
    {
        super(box);

        int startpos = reader.getCurrentPosition();
        int endpos = startpos + available();

        colourType = new String(reader.readBytes(4), StandardCharsets.UTF_8);
        
        if (colourType.equals("nclx"))
        {
            // Check if enough bytes are available for nclx specific data (3 shorts + 1 byte = 7
            // bytes)

            if (available() < 7)
            {
                LOGGER.warn("Not enough bytes for [nclx] ColourInformationBox. Expected 7, but found [" + available() + "]. Box may be malformed");

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
            System.out.printf("%s\n", colourType);
            
            /*
             * Both restricted ICC profile ('rICC') and unrestricted ICC profile ('prof') are
             * currently not used, therefore we just skip them safely.
             */

            if (available() > 0)
            {
                // For now, read silently and safely
                this.iccProfileData = reader.readBytes(available());

                LOGGER.info("Skipping [" + available() + "] bytes of ICC profile data for [" + colourType + "] colour type");
            }
        }

        else if (available() > 0)
        {
            reader.skip(available());

            LOGGER.warn("Unknown colourType [" + colourType + "] encountered in ColourInformationBox. Skipping remaining [" + available() + "] bytes");
        }

        if (reader.getCurrentPosition() != endpos)
        {
            LOGGER.warn("ColourInformationBox for type [" + colourType + "] did not consume all expected payload bytes. Remaining: [" + (endpos - reader.getCurrentPosition()) + "]");

            throw new IllegalStateException("Mismatch in expected box size for [" + getTypeAsString() + "]");
        }

        byteUsed += reader.getCurrentPosition() - startpos;
    }

    /**
     * Returns the 4-character string identifying the colour type. Examples include "nclx", "rICC",
     * "prof".
     *
     * @return the colour type string
     */
    public String getColourType()
    {
        return colourType;
    }

    /**
     * Returns the colour primaries value for 'nclx' colour types. This value defines the
     * chromaticity of the primaries and the white point.
     *
     * @return the colour primaries, or 0 if not an 'nclx' type or not parsed
     */
    public int getColourPrimaries()
    {
        return colourPrimaries;
    }

    /**
     * Returns the transfer characteristics value for 'nclx' colour types. This value defines the
     * opto-electronic transfer characteristic of the source picture.
     *
     * @return the transfer characteristics, or 0 if not an 'nclx' type or not parsed
     */
    public int getTransferCharacteristics()
    {
        return transferCharacteristics;
    }

    /**
     * Returns the matrix coefficients value for 'nclx' colour types. This value defines the matrix
     * coefficients used in deriving luminance and chrominance signals.
     *
     * @return the matrix coefficients, or 0 if not an 'nclx' type or not parsed
     */
    public int getMatrixCoefficients()
    {
        return matrixCoefficients;
    }

    /**
     * Returns the full range flag for 'nclx' colour types. {@code true} indicates full range
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
        return iccProfileData;
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
            sb.append(prefix).append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }

        sb.append(String.format("%s '%s': Type=%s", this.getClass().getSimpleName(), getTypeAsString(), colourType));

        if (colourType.equals("nclx"))
        {
            sb.append(String.format(", Primaries=0x%04X, Transfer=0x%04X, Matrix=0x%04X, FullRange=%b", colourPrimaries, transferCharacteristics, matrixCoefficients, isFullRangeFlag));
        }

        else if (colourType.equals("rICC") || colourType.equals("prof"))
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