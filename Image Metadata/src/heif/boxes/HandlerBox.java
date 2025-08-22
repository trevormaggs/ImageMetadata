package heif.boxes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import common.ByteValueConverter;
import common.SequentialByteReader;
import logger.LogFactory;

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
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class HandlerBox extends FullBox
{
    private static final LogFactory LOGGER = LogFactory.getLogger(HandlerBox.class);
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
        LOGGER.debug(String.format("%s%s '%s':\t\t\t'%s'", tab, this.getClass().getSimpleName(), getTypeAsString(), getHandlerType()));
    }
}