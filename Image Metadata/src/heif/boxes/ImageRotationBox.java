package heif.boxes;

import common.SequentialByteReader;
import logger.LogFactory;

/**
 * This derived Box class handles the Box identified as {@code irot} - Image rotation Box. For
 * technical details, refer to the Specification document -
 * {@code ISO/IEC 23008-12:2017 in Page 15}.
 *
 * The image rotation transformative item property of the {@code ImageRotationBox} box rotates the
 * reconstructed image of the associated image item in anti-clockwise direction in units of 90
 * degrees.
 *
 * <p>
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class ImageRotationBox extends Box
{
    private static final LogFactory LOGGER = LogFactory.getLogger(ImageRotationBox.class);
    private final int angle;
    private final int reserved;

    /**
     * This constructor creates a derived Box object whose aim is to retrieve the angle (in
     * anti-clockwise direction) in units of degrees.
     *
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public ImageRotationBox(Box box, SequentialByteReader reader)
    {
        super(box);

        long pos = reader.getCurrentPosition();
        int data = reader.readUnsignedByte();

        // First 6 bits are reserved
        reserved = data & 0xFC;
        angle = data & 0x03;

        byteUsed += reader.getCurrentPosition() - pos;
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
        LOGGER.debug(String.format("%s%s '%s': angle=%d, reserved=%d", tab, this.getClass().getSimpleName(), getTypeAsString(), angle, reserved));
    }
}