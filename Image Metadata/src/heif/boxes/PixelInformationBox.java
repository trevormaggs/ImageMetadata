package heif.boxes;

import java.util.Arrays;
import common.SequentialByteReader;

/**
 * This derived Box class handles the Box identified as {@code pixi} - Pixel information Box. For
 * technical details, refer to the Specification document -
 * {@code ISO/IEC 23008-12:2017 in Page 13}.
 * 
 * The {@code PixelInformationProperty} descriptive item property indicates the number and bit depth
 * of colour components in the reconstructed image of the associated image item.
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
public class PixelInformationBox extends FullBox
{
    int numChannels;
    int[] bitsPerChannel;

    /**
     * This constructor creates a derived Box object to augment the item property list.
     * 
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public PixelInformationBox(Box box, SequentialByteReader reader)
    {
        super(box, reader);

        int pos = reader.getCurrentPosition();

        numChannels = reader.readUnsignedByte();
        bitsPerChannel = new int[numChannels];

        for (int i = 0; i < bitsPerChannel.length; i++)
        {
            bitsPerChannel[i] = reader.readUnsignedByte();
        }

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

        line.append(String.format("\t\t%s '%s': numChannels=%s, bitsPerChannel=%s", this.getClass().getSimpleName(), getBoxName(), numChannels, Arrays.toString(bitsPerChannel)));

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
        line.append(String.format("  %-24s %s%n", "[Num Channels]", numChannels));
        line.append(String.format("  %-24s %s%n", "[Bits Per Channel]", Arrays.toString(bitsPerChannel)));

        return line.toString();
    }
}