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
 * <p>
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 *
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 31 May 2025</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 31 May 2025
 */
public class HandlerBox extends FullBox
{
    private final String name;
    private final byte[] handlerType;

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

        long pos = reader.getCurrentPosition();

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
        name = new String(ByteValueConverter.readFirstNullTerminatedByteArray(b), StandardCharsets.UTF_8);

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
     * Returns a string representation of this {@code HandlerBox}.
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
            sb.append(prefix);
        }

        for (int i = 0; i < getHierarchyDepth(); i++)
        {
            sb.append("\t");
        }

        sb.append(String.format("%s '%s':\t\t\t'%s'", this.getClass().getSimpleName(), getTypeAsString(), getHandlerType()));
        sb.append(System.lineSeparator());

        return sb.toString();
    }
}