package heif.boxes;

import java.nio.charset.StandardCharsets;
import common.SequentialByteReader;

/**
 * This derived class handles the Box identified as {@code colr} - Colour information Box. For
 * technical details, refer to the Specification document - ISO/IEC 14496-12:2015 in Page 158.
 * 
 * <p>
 * Version History:
 * </p>
 * 
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 28 May 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 28 May 2025
 */
public class ColourInformationBox extends Box
{
    String colourType;
    int colourPrimaries;
    int transferCharacteristics;
    int matrixCoefficients;
    boolean fullRangeFlag;

    public ColourInformationBox(Box box, SequentialByteReader reader)
    {
        super(box);

        int pos = reader.getCurrentPosition();

        colourType = new String(reader.readBytes(4), StandardCharsets.UTF_8);

        if (colourType.equals("nclx"))
        {
            colourPrimaries = reader.readUnsignedShort();
            transferCharacteristics = reader.readUnsignedShort();
            matrixCoefficients = reader.readUnsignedShort();

            int bits = reader.readByte();
            fullRangeFlag = (((bits & 0x80) >> 7) == 1);

            // Last 7 bits are reserved
            // int reserved = (bits & 0x7F);
        }

        else if (colourType.equals("rICC"))
        {
            // restricted ICC profile - currently not used
        }

        else if (colourType.equals("prof"))
        {
            // unrestricted ICC profile - currently not used
        }

        byteUsed += reader.getCurrentPosition() - pos;
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
     * Returns a human-readable debug string, summarising structured references associated with this
     * HEIF-based file. Useful for logging or diagnostics.
     *
     * @param prefix
     *        Optional heading or label to prepend. Can be {@code null}.
     * 
     * @return A formatted string suitable for debugging, inspection, or textual analysis
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

        sb.append(String.format("%s '%s': colourType=%s'", this.getClass().getSimpleName(), getTypeAsString(), colourType));

        return sb.toString();
    }
}