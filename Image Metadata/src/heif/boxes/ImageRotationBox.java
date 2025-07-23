package heif.boxes;

import common.SequentialByteReader;

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
public class ImageRotationBox extends Box
{
    int angle;
    int reserved;

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

        int pos = reader.getCurrentPosition();
        int data = reader.readUnsignedByte();

        // First 6 bits are reserved
        reserved = data & 0xFC;
        angle = data & 0x03;

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns a string representation of this {@code ImageRotationBox}.
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

        sb.append(String.format("%s '%s': angle=%d", this.getClass().getSimpleName(), getTypeAsString(), angle));

        return sb.toString();
    }
}