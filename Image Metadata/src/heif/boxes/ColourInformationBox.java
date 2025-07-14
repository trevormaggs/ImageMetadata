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
     * Displays a list of structured references associated with the specified HEIF based file,
     * useful for analytical purposes.
     *
     * @return the string
     */
    @Override
    public String showBoxStructure()
    {
        StringBuilder line = new StringBuilder();

        line.append(String.format("\t\t%s '%s': colourType=%s'", this.getClass().getSimpleName(), getBoxName(), colourType));

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

        line.append(String.format("  %-24s %s%n", "[Colour Type]", colourType));
        line.append(String.format("  %-24s %s%n", "[Colour Primaries]", colourPrimaries));
        line.append(String.format("  %-24s %s%n", "[Trx. Characteristics]", transferCharacteristics));
        line.append(String.format("  %-24s %s%n", "[Matrix Coefficients]", matrixCoefficients));
        line.append(String.format("  %-24s %s%n", "[Full Range Flag]", fullRangeFlag));

        return line.toString();
    }
}