package heif.boxes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import common.ByteValueConverter;
import common.SequentialByteReader;

/**
 * This derived Box class handles the Box identified as {@code auxc} - Image properties for
 * auxiliary images Box. For technical details, refer to the Specification document -
 * {@code ISO/IEC 23008-12:2017 in Page 14}.
 *
 * Auxiliary images shall be associated with an {@code AuxiliaryTypeProperty} as defined here. It
 * includes a URN identifying the type of the auxiliary image. it may also include other fields, as
 * required by the URN.
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
 * @implNote Additional testing is required to validate the reliability and robustness of this
 *           implementation
 */
public class AuxiliaryTypePropertyBox extends FullBox
{
    String auxType;
    byte[] auxSubtype;

    /**
     * This constructor creates a derived Box object to augment the item property list.
     * 
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public AuxiliaryTypePropertyBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        auxSubtype = reader.readBytes(available());
        auxType = new String(ByteValueConverter.trimNullTerminatedByteArray(auxSubtype), StandardCharsets.UTF_8);

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

        line.append(String.format("\t\t%s '%s': auxType=%s", this.getClass().getSimpleName(), getBoxName(), auxType));

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
        line.append(String.format("  %-24s %s%n", "[Auxiliary Type]", auxType));
        line.append(String.format("  %-24s %s%n", "[Auxiliary Sub Type]", Arrays.toString(auxSubtype)));

        return line.toString();
    }
}