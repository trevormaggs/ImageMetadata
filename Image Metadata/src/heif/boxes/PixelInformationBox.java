package heif.boxes;

import java.util.Arrays;
import common.SequentialByteReader;
import logger.LogFactory;

/**
 * Represents the {@code pixi} (Pixel Information Box), which provides bit depth and number of
 * channels for a reconstructed image.
 *
 * <p>
 * Specification Reference: ISO/IEC 23008-12:2017 on Page 13.
 * </p>
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
public class PixelInformationBox extends FullBox
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PixelInformationBox.class);
    private final int numChannels;
    private final int[] bitsPerChannel;

    /**
     * Constructs a {@code PixelInformationBox} by parsing the specified box header and its content.
     *
     * @param box
     *        the parent {@link Box} containing size and type information
     * @param reader
     *        the reader for parsing box content
     *
     * @throws IllegalStateException
     *         if malformed data is encountered
     */
    public PixelInformationBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        long pos = reader.getCurrentPosition();

        numChannels = reader.readUnsignedByte();

        if (numChannels <= 0 || numChannels > 255)
        {
            throw new IllegalStateException("Channel count must be between 0 and 255. Found [" + numChannels + "]");
        }

        bitsPerChannel = new int[numChannels];

        for (int i = 0; i < bitsPerChannel.length; i++)
        {
            bitsPerChannel[i] = reader.readUnsignedByte();
        }

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns the number of image channels described by this box.
     *
     * @return the number of channels
     */
    public int getNumChannels()
    {
        return numChannels;
    }

    /**
     * Returns a copy of the array of bits per channel.
     *
     * @return bits per channel array
     */
    public int[] getBitsPerChannel()
    {
        return bitsPerChannel.clone();
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
        LOGGER.debug(String.format("%s%s '%s': numChannels=%s, bitsPerChannel=%s", tab, this.getClass().getSimpleName(), getTypeAsString(), numChannels, Arrays.toString(bitsPerChannel)));
    }
}