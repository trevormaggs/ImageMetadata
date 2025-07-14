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
    public long imageWidth;
    public long imageHeight;

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
     * Displays a list of structured references associated with the specified HEIF based file,
     * useful for analytical purposes.
     *
     * @return the string
     */
    @Override
    public String showBoxStructure()
    {
        StringBuilder line = new StringBuilder();

        line.append(String.format("\t\t%s '%s': imageWidth=%d, imageHeight=%d", this.getClass().getSimpleName(), getBoxName(), imageWidth, imageHeight));

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

        line.append(String.format("  %-24s %s%n", "[Image Width]", imageWidth));
        line.append(String.format("  %-24s %s%n", "[Image Height]", imageHeight));

        return line.toString();
    }
}