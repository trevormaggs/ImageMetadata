package heif.boxes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import common.SequentialByteReader;

/**
 * The {@code FileTypeBox} must be the first box in every HEIF-based file, including HEIC still
 * image files. It provides high-level file type information, including the major brand, minor
 * version, and a list of compatible brands.
 * 
 * For technical details, refer to ISO/IEC 14496-12:2015, Page 7 (File Type Box).
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
 *           implementation.
 */
public class FileTypeBox extends Box
{
    private final byte[] majorBrand;
    private final long minorVersion;
    private final List<String> compatibleBrands;

    /**
     * Constructs a {@code FileTypeBox}, parsing its fields from the specified
     * {@link SequentialByteReader}.
     *
     * @param box
     *        the parent {@link Box} object containing size and type information
     * @param reader
     *        the byte reader for parsing box data
     */
    public FileTypeBox(Box box, SequentialByteReader reader)
    {
        super(box);

        int pos = reader.getCurrentPosition();

        compatibleBrands = new ArrayList<>();
        majorBrand = reader.readBytes(4);
        minorVersion = reader.readUnsignedInteger();

        int remaining = available() - 8; // 4 bytes majorBrand + 4 bytes minorVersion

        /*
         * Compatible brands start after the major brand and minor version.
         * Each compatible brand is 4 bytes.
         */
        for (int i = 0; i < remaining; i += 4)
        {
            compatibleBrands.add(new String(reader.readBytes(4), StandardCharsets.UTF_8));
        }

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Returns the primary brand identifier. Typically, for HEIC-based files, the major brand is
     * {@code heic}.
     *
     * @return the primary brand as a string
     */
    public String getMajorBrand()
    {
        return new String(majorBrand, StandardCharsets.UTF_8);
    }

    /**
     * Returns the minor version associated with the major brand.
     *
     * @return the minor version (usually 0 for HEIC files)
     */
    public int getMinorVersion()
    {
        return (int) minorVersion;
    }

    /**
     * Returns an array of compatible brands as specified in the box, excluding the major brand.
     *
     * @return an array of compatible brand strings
     */
    public String[] getCompatibleBrands()
    {
        return compatibleBrands.toArray(new String[0]);
    }

    /**
     * Checks if the specified brand is listed in the compatible brands.
     *
     * @param brand
     *        the brand name to check
     * 
     * @return boolean true if the brand is present, otherwise false
     */
    public boolean hasBrand(String brand)
    {
        for (String s : compatibleBrands)
        {
            if (s.equalsIgnoreCase(brand))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a string representation of this {@code FileTypeBox} resource.
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

        sb.append(String.format("%s '%s':\t\t\t\t", this.getClass().getSimpleName(), getTypeAsString()));
        sb.append(String.format("'major-brand=%s', ", getMajorBrand()));
        sb.append(String.format("compatible-brands='%s'", Arrays.toString(getCompatibleBrands())));
        // sb.append(System.lineSeparator());

        return sb.toString();
    }
}