package heif.boxes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import common.ByteValueConverter;
import common.SequentialByteReader;

/**
 * This derived box, namely the {@code hdlr} type, declares media type of the track, and the process
 * by which the media-data in the track is presented. Typically, this is the contained box within
 * the parent {@code meta} box.
 *
 * This object consumes a total of 20 bytes, in addition to the variable length of the name string.
 * Exactly one instance of the {@code hdlr} box should exist.
 *
 * This implementation follows to the guidelines outlined in the Specification -
 * {@code ISO/IEC 14496-12:2015} on Page 29, and also {@code ISO/IEC 23008-12:2017 on Page 22}.
 *
 * <p>
 * Version History:
 * </p>
 *
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 31 May 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 31 May 2025
 * @implNote Additional testing is required to confirm the reliability and robustness of this
 *           implementation
 */
public class HandlerBox extends FullBox
{
    private String name;
    private byte[] handlerType;

    /**
     * This constructor creates a derived Box object, extending the super class {@code FullBox} to
     * provide more specific information about this box.
     *
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public HandlerBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        /* Pre-defined = 0 */
        reader.skip(4);

        /* May be null */
        handlerType = reader.readBytes(4);

        /* Reserved = 0 */
        reader.skip(12);

        /*
         * Human-readable name for the track type
         * (for debugging and inspection purposes).
         * 
         * Subtract the required length by 32 bytes because:
         * 
         * 4 bytes - Length
         * 4 bytes - Box Type
         * 4 bytes - from FullBox
         * 20 bytes - from this box
         */
        byte[] b = reader.readBytes((int) box.getBoxSize() - 32);
        name = new String(ByteValueConverter.trimNullTerminatedByteArray(b), StandardCharsets.UTF_8);

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns a string representation of the Handler Type, providing information about the media
     * type for movie tracks or format type for meta box contents.
     *
     * @return the Handler Type as a string
     */
    public String getHandlerType()
    {
        return new String(handlerType, StandardCharsets.UTF_8);
    }

    /**
     * Returns a human-readable name for the track type, useful for debugging and inspection
     * purposes.
     *
     * @return string
     */
    public String getName()
    {
        return (name == null || name.isEmpty() ? "<Empty>" : name);
    }

    /**
     * Checks whether the handler type for still images or image sequences is the {@code pict} type.
     *
     * @return a boolean value of true if the handler is set for the {@code pict} type, otherwise
     *         false
     */
    public boolean containsPictHandler()
    {
        return Arrays.equals(handlerType, "pict".getBytes()) ? true : false;
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

        line.append(String.format("\t%s '%s':\t\t\t'%s'", this.getClass().getSimpleName(), getBoxName(), getHandlerType()));

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
        line.append(String.format("  %-24s %s%n", "[Handler Type]", getHandlerType()));
        line.append(String.format("  %-24s %s%n", "[Name]", getName()));

        return line.toString();
    }
}