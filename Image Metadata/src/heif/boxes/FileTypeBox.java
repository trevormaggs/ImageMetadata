package heif.boxes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import common.SequentialByteReader;

/**
 * The File Type Box must be the first identifier in every HEIF based file, including the HEIC
 * still image files. It inherits from the Box superclass, providing key high-level data for other
 * boxes have access to.
 * 
 * For comprehensive technical details, consult the Specification document - ISO/IEC 14496-12:2015
 * on Page 7 under {@code File Type Box}.
 *
 * <p>
 * Version History:
 * </p>
 *
 * <ul>
 * <li>1.0 - Initial release by Trevor Maggs on 31 May 2025</li>
 * </ul>
 *
 * @implNote Additional testing is required to confirm the reliability and robustness of this
 *           implementation
 * @author Trevor Maggs
 * @since 31 May 2025
 */
public class FileTypeBox extends Box
{
    private static final short START_BRAND_POSITION = 16;
    private byte[] majorBrand;
    private long minorVersion;
    private List<String> compatibleBrands;

    /**
     * This constructor creates a derived Box object, extending the super class {@code Box} to
     * provide additional information.
     *
     * @param box
     *        the super Box object
     * @param reader
     *        a SequentialByteReader object for sequential byte array access
     */
    public FileTypeBox(Box box, SequentialByteReader reader)
    {
        super(box);

        int pos = reader.getCurrentPosition();

        compatibleBrands = new ArrayList<>();
        majorBrand = reader.readBytes(4);
        minorVersion = reader.readUnsignedInteger();

        /*
         * Compatible brands start from byte position 16.
         * 4 bytes - Length
         * 4 bytes - Box Type
         * 4 bytes - Major Brand
         * 4 bytes - Minor Version
         * Subsequent 4 byte blocks - Compatible brands, which start from position 16 until the end
         * of that box
         */
        for (int i = START_BRAND_POSITION; i < getBoxSize(); i += 4)
        {
            compatibleBrands.add(new String(reader.readBytes(4), StandardCharsets.UTF_8).toLowerCase());
        }

        byteUsed += reader.getCurrentPosition() - pos;
    }

    /**
     * Retrieves the primary brand identifier. Typically, for HEIC-based files, the major brand is
     * labeled as {@code heic}.
     *
     * @return the primary brand identifier as a string
     */
    public String getMajorBrand()
    {
        return new String(majorBrand, StandardCharsets.UTF_8);
    }

    /**
     * Retrieves the integer representing the minor version of the major brand.
     *
     * @return the version as an integer. In most cases, the value is 0
     */
    public int getMinorVersion()
    {
        return (int) minorVersion;
    }

    /**
     * Returns an array of other compatible brands, including the major brand.
     *
     * @return the array of brand strings
     */
    public String[] getCompatibleBrands()
    {
        return compatibleBrands.toArray(new String[compatibleBrands.size()]);
    }

    /**
     * Validates the specified brand is present in the list of compatible brands.
     *
     * @param brand
     *        the brand name to check
     *        
     * @return boolean true if the brand has a presence, otherwise false
     */
    public boolean hasBrand(String brand)
    {
        return compatibleBrands.contains(brand.toLowerCase());
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

        line.append(String.format("%s '%s':\t\t\t\t", this.getClass().getSimpleName(), getBoxName()));
        line.append(String.format("'major-brand=%s', ", getMajorBrand()));
        line.append(String.format("compatible-brands='%s'", Arrays.toString(getCompatibleBrands())));

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
        line.append(String.format("  %-24s %s%n", "[Major Brand]", getMajorBrand()));
        line.append(String.format("  %-24s %s%n", "[Minor Version]", getMinorVersion()));
        line.append(String.format("  %-24s %s%n", "[Brands]", Arrays.toString(getCompatibleBrands())));

        return line.toString();
    }
}