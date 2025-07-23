package heif.boxes;

import java.nio.charset.StandardCharsets;
import common.ByteValueConverter;
import common.SequentialByteReader;

/**
 * Represents the {@code auxc} (Auxiliary Type Property Box), providing auxiliary image type
 * information.
 *
 * Auxiliary images shall be associated with an {@code AuxiliaryTypeProperty} as defined here. It
 * includes a URN identifying the type of the auxiliary image. it may also include other fields, as
 * required by the URN.
 * 
 * <p>
 * Specification Reference: ISO/IEC 23008-12:2017 on Page 14
 * </p>
 * 
 * <p>
 * Version History:
 * </p>
 * <ul>
 * <li>1.0 – Initial release by Trevor Maggs on 2 June 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 2 June 2025
 */
public class AuxiliaryTypePropertyBox extends FullBox
{
    private final String auxType;
    private final byte[] auxSubtype;

    /**
     * Constructs an {@code AuxiliaryTypePropertyBox} from the box header and content.
     *
     * @param box
     *        the parent {@link Box}
     * @param reader
     *        the byte reader
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
     * Returns the auxiliary type string (URN or similar).
     *
     * @return auxiliary type
     */
    public String getAuxType()
    {
        return auxType;
    }

    /**
     * Returns the raw auxSubtype bytes, which may contain additional parameters after the
     * null-terminated string.
     *
     * @return a copy of the auxSubtype bytes
     */
    public byte[] getAuxSubtype()
    {
        return auxSubtype.clone();
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

        sb.append(String.format("%s '%s': auxType=%s", this.getClass().getSimpleName(), getTypeAsString(), auxType));

        return sb.toString();
    }
}