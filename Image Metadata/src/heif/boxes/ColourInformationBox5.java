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
public class ColourInformationBox5 extends Box
{

    private static final LogFactory LOGGER = LogFactory.getLogger(ColourInformationBox5.class);
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
     */
    public ColourInformationBox5(Box box, SequentialByteReader reader)
    {
        super(box);

        int startPos = reader.getCurrentPosition();
        int remainingPayloadBytes = available();

        // Read 4-byte colourType
        colourType = new String(reader.readBytes(4), StandardCharsets.UTF_8);
        remainingPayloadBytes -= 4;

        if (colourType.equals("nclx"))
        {
            // For nclx specific data (3 shorts + 1 byte = 7 bytes
            if (remainingPayloadBytes < 7)
            {
                LOGGER.warn("Not enough bytes for [nclx] ColourInformationBox. Expected 7, but found [" + remainingPayloadBytes + "]. Box may be malformed");

                throw new IllegalStateException("Mismatch in expected box size for [" + getTypeAsString() + "]");
            }

            colourPrimaries = reader.readUnsignedShort();
            transferCharacteristics = reader.readUnsignedShort();
            matrixCoefficients = reader.readUnsignedShort();

            int bits = reader.readByte();
            isFullRangeFlag = (((bits & 0x80) >> 7) == 1);
            // int reserved = (bits & 0x7F); // The last 7 bits are reserved, can be ignored or
            // stored

            remainingPayloadBytes -= 7; // Account for the 7 bytes read for nclx data
        }
        else if (colourType.equals("rICC") || colourType.equals("prof"))
        {
            // For 'rICC' (restricted ICC profile) and 'prof' (unrestricted ICC profile),
            // the rest of the box payload contains the ICC profile data.
            // This implementation currently skips this data as its parsing is out of scope.
            if (remainingPayloadBytes > 0)
            {
                // If you need to store the ICC profile, uncomment the line below and add the field.
                // this.iccProfileData = reader.readBytes(remainingPayloadBytes);
                reader.skip(remainingPayloadBytes); // Skip the remaining bytes of the ICC profile

                LOGGER.info("Skipping [" + remainingPayloadBytes + "] bytes of ICC profile data for [" + colourType + "] colour type");
            }

            remainingPayloadBytes = 0;
        }

        else
        {
            LOGGER.warn("Unknown colourType [" + colourType + "] encountered in ColourInformationBox. Skipping remaining [" + remainingPayloadBytes + "] bytes");

            if (remainingPayloadBytes > 0)
            {
                reader.skip(remainingPayloadBytes); // Skip any unparsed bytes for unknown types
            }

            remainingPayloadBytes = 0;
        }

        // Final check to ensure all expected bytes were consumed from the box's payload.
        // If remainingPayloadBytes is not 0, it means there's a discrepancy
        // between the box's reported size and what was actually parsed/skipped.
        if (remainingPayloadBytes != 0)
        {
            LOGGER.warn("ColourInformationBox for type [" + colourType + "] did not consume all expected payload bytes. Remaining: [" + remainingPayloadBytes + "]");

            throw new IllegalStateException("Mismatch in expected box size for [" + getTypeAsString() + "]");
        }

        byteUsed += reader.getCurrentPosition() - startPos; // Update total bytes used by this box
    }

    /**
     * Returns the 4-character string identifying the colour type.
     * Examples include "nclx", "rICC", "prof".
     *
     * @return the colour type string.
     */
    public String getColourType()
    {
        return colourType;
    }

    /**
     * Returns the colour primaries value for 'nclx' colour types.
     * This value defines the chromaticity of the primaries and the white point.
     *
     * @return the colour primaries, or 0 if not an 'nclx' type or not parsed.
     */
    public int getColourPrimaries()
    {
        return colourPrimaries;
    }

    /**
     * Returns the transfer characteristics value for 'nclx' colour types.
     * This value defines the opto-electronic transfer characteristic of the source picture.
     *
     * @return the transfer characteristics, or 0 if not an 'nclx' type or not parsed.
     */
    public int getTransferCharacteristics()
    {
        return transferCharacteristics;
    }

    /**
     * Returns the matrix coefficients value for 'nclx' colour types.
     * This value defines the matrix coefficients used in deriving luminance and chrominance
     * signals.
     *
     * @return the matrix coefficients, or 0 if not an 'nclx' type or not parsed.
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
     * Returns a human-readable debug string, summarising the colour information
     * contained in this {@code ColourInformationBox}. Useful for logging or diagnostics.
     *
     * @param prefix
     *        Optional heading or label to prepend. Can be {@code null}.
     *
     * @return a formatted string suitable for debugging, inspection, or textual analysis.
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

        sb.append(String.format("%s '%s': Type=%s",
                this.getClass().getSimpleName(), getTypeAsString(), colourType));

        if (colourType.equals("nclx"))
        {
            sb.append(String.format(", Primaries=0x%04X, Transfer=0x%04X, Matrix=0x%04X, FullRange=%b",
                    colourPrimaries, transferCharacteristics, matrixCoefficients, isFullRangeFlag));
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