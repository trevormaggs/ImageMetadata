package heif.boxes;

import java.nio.charset.StandardCharsets;
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
     * Returns a string representation of this {@code AuxiliaryTypePropertyBox}.
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

        sb.append(String.format("\t\t%s '%s': auxType=%s", this.getClass().getSimpleName(), getTypeAsString(), auxType));

        return sb.toString();
    }
}