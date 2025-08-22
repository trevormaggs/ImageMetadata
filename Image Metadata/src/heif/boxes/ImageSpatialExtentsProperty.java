package heif.boxes;

import common.SequentialByteReader;
import logger.LogFactory;

/**
 * This derived Box class handles the Box identified as {@code ispe} - Image spatial extents Box.
 * For technical details, refer to the Specification document -
 * {@code ISO/IEC 23008-12:2017 in Page 11}.
 *
 * The {@code ImageSpatialExtentsProperty} Box records the width and height of the associated image
 * item. Every image item shall be associated with one property of this type, prior to the
 * association of all transformative properties.
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
public class ImageSpatialExtentsProperty extends FullBox
{
    private static final LogFactory LOGGER = LogFactory.getLogger(ImageSpatialExtentsProperty.class);
    public final long imageWidth;
    public final long imageHeight;

    /**
     * This constructor creates a derived Box object whose aim is to gather information both on
     * width and height of the associated image item.
     *
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public ImageSpatialExtentsProperty(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        long pos = reader.getCurrentPosition();

        imageWidth = reader.readUnsignedInteger();
        imageHeight = reader.readUnsignedInteger();

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
        LOGGER.debug(String.format("%s%s '%s': imageWidth=%d, imageHeight=%d", tab, this.getClass().getSimpleName(), getTypeAsString(), imageWidth, imageHeight));
    }
}