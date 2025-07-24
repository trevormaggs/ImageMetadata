package heif.boxes;

import common.SequentialByteReader;

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
public class ImageSpatialExtentsProperty extends FullBox
{
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

        int pos = reader.getCurrentPosition();

        imageWidth = reader.readUnsignedInteger();
        imageHeight = reader.readUnsignedInteger();

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns a string representation of this {@code ImageSpatialExtentsProperty}.
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

        sb.append(String.format("%s '%s': imageWidth=%d, imageHeight=%d", this.getClass().getSimpleName(), getTypeAsString(), imageWidth, imageHeight));

        return sb.toString();
    }
}