package heif.boxes;

import java.util.Arrays;

import common.SequentialByteReader;

/**
 * Represents the {@code pixi} (Pixel Information Box), which provides bit depth and number of
 * channels for a reconstructed image.
 * 
 * <p>
 * Specification Reference: ISO/IEC 23008-12:2017 on Page 13.
 * </p>
 * 
 * <p>
 * Version History:
 * </p>
 * 
 * <ul>
 * <li>1.0 – Initial release by Trevor Maggs on 2 June 2025</li>
 * </ul>
 * 
 * <p>
 * <strong>API Note:</strong> Additional testing is required to validate the reliability and
 * robustness of this implementation.
 * </p>
 * 
 * @author Trevor Maggs
 * @since 2 June 2025
 */
public class PixelInformationBox extends FullBox
{
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

        int pos = reader.getCurrentPosition();

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
     * Returns a string representation of this {@code PixelInformationBox} resource.
     *
     * @return a formatted string describing the box contents
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
            sb.append(prefix);
        }

        for (int i = 0; i < getHierarchyDepth(); i++)
        {
            sb.append("\t");
        }

        sb.append(String.format("%s '%s': numChannels=%s, bitsPerChannel=%s", this.getClass().getSimpleName(), getTypeAsString(), numChannels, Arrays.toString(bitsPerChannel)));
        sb.append(System.lineSeparator());

        return sb.toString();
    }
}