package heif.boxes;

import java.nio.charset.StandardCharsets;
import common.ByteValueConverter;
import common.SequentialByteReader;
import logger.LogFactory;

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
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class AuxiliaryTypePropertyBox extends FullBox
{
    private static final LogFactory LOGGER = LogFactory.getLogger(AuxiliaryTypePropertyBox.class);
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

        long pos = reader.getCurrentPosition();

        auxSubtype = reader.readBytes(available());
        auxType = new String(ByteValueConverter.readFirstNullTerminatedByteArray(auxSubtype), StandardCharsets.UTF_8);

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
     * Logs a single diagnostic line for this box at the debug level.
     *
     * <p>
     * This is useful when traversing the box tree of a HEIF/ISO-BMFF structure for debugging or
     * inspection purposes.
     * </p>
     */
    @Override
    public void logBoxInfo()
    {
        String tab = Box.repeatPrint("\t", getHierarchyDepth());
        LOGGER.debug(String.format("%s%s '%s': auxType=%s", tab, this.getClass().getSimpleName(), getTypeAsString(), auxType));
    }
}