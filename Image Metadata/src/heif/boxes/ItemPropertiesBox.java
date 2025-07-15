package heif.boxes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import common.SequentialByteReader;
import heif.BoxFactory;
import heif.HeifBoxType;

/**
 * Represents the {@code iprp} (Item Properties Box) in a HEIF/HEIC file structure.
 * 
 * <p>
 * The {@code ItemPropertiesBox} allows the definition of properties that describe specific
 * characteristics of media items, such as images or auxiliary data. Each property is stored
 * in the {@code ipco} (ItemPropertyContainerBox), while associations between items and their
 * properties are managed through one or more {@code ipma} (ItemPropertyAssociationBox) entries.
 * </p>
 * 
 * <p>
 * <b>Box Structure:</b>
 * </p>
 * 
 * <ul>
 * <li>{@code ipco} – Contains an implicitly indexed list of property boxes.</li>
 * <li>{@code ipma} – Maps items to property indices defined in {@code ipco}.</li>
 * </ul>
 * 
 * <p>
 * <b>Specification Reference:</b>
 * </p>
 * <ul>
 * <li>ISO/IEC 23008-12:2017, Section 6.5.5 (Page 28)</li>
 * </ul>
 * 
 * <p>
 * <b>Version History:</b>
 * </p>
 * <ul>
 * <li>1.0 – Initial release by Trevor Maggs on 2 June 2025</li>
 * </ul>
 * 
 * @author Trevor Maggs
 * @since 2 June 2025
 * @implNote This implementation assumes a flat box hierarchy. Additional testing is recommended
 *           for nested or complex structures.
 */
public class ItemPropertiesBox extends Box
{
    private ItemPropertyContainerBox ipco;
    private List<ItemPropertyAssociationBox> associations;

    /**
     * Represents the {@code ipco} (ItemPropertyContainerBox), a nested container holding an
     * implicitly indexed list of item property boxes.
     * 
     * <p>
     * Each property describes an aspect of an image or media item, such as color information, pixel
     * layout, or transformation metadata.
     * </p>
     * 
     * <p>
     * Refer to the Specification document - {@code ISO/IEC 23008-12:2017} on Page 28 for more
     * information.
     * </p>
     */
    private static class ItemPropertyContainerBox extends Box
    {
        private List<Box> properties;

        /**
         * Constructs an {@code ItemPropertyContainerBox} resource by reading sequential boxes from
         * the {@code SequentialByteReader}.
         * 
         * <p>
         * Each property box is read, added to the property list, and skipped over to handle cases
         * where specific handlers for sub-boxes may not yet be implemented.
         * </p>
         * 
         * @param box
         *        the parent Box containing size and header information
         * @param reader
         *        the sequential byte reader for parsing box data
         * 
         * @throws IllegalArgumentException
         *         if a sub-box reports a negative size (corrupted file)
         */
        private ItemPropertyContainerBox(Box box, SequentialByteReader reader)
        {
            super(box);

            int startpos = reader.getCurrentPosition();
            int endpos = startpos + available();

            properties = new ArrayList<>();

            do
            {
                Box newBox = BoxFactory.createBox(reader);

                if (newBox.available() < 0)
                {
                    throw new IllegalArgumentException("Negative box size detected at [" + newBox.getBoxName() + "]");
                }

                /*
                 * Skip the content to allow parsing unhandled sub-boxes safely.
                 * hvcC box is one of them.
                 */
                reader.skip(newBox.available());

                properties.add(newBox);

            } while (reader.getCurrentPosition() < endpos);

            if (reader.getCurrentPosition() != endpos)
            {
                throw new IllegalStateException("Mismatch in expected box size for ipco");
            }

            byteUsed += reader.getCurrentPosition() - startpos;
        }
    }

    /**
     * Constructs an {@code ItemPropertiesBox} by reading the {@code ipco} (property container) and
     * one or more {@code ipma} (item-property association) boxes.
     * 
     * The ItemPropertiesBox consists of two parts: {@code ItemPropertyContainerBox} that contains
     * an implicitly indexed list of item properties, and one or more ItemPropertyAssociation boxes
     * that associate items with item properties.
     * 
     * @param box
     *        the parent Box header containing size and type
     * @param reader
     *        a {@code SequentialByteReader} to read the box content
     * 
     * @throws IllegalArgumentException
     *         if malformed data is encountered, such as a negative box size
     */
    public ItemPropertiesBox(Box box, SequentialByteReader reader)
    {
        super(box);

        int startpos = reader.getCurrentPosition();
        int endpos = startpos + available();

        associations = new ArrayList<>();

        ipco = new ItemPropertyContainerBox(new Box(reader), reader);

        do
        {
            associations.add(new ItemPropertyAssociationBox(new Box(reader), reader));

        } while (reader.getCurrentPosition() < endpos);

        if (reader.getCurrentPosition() != endpos)
        {
            throw new IllegalStateException("Mismatch in expected box size for iprp");
        }

        byteUsed += reader.getCurrentPosition() - startpos;
    }

    /**
     * Retrieves the list of property boxes contained within the {@code ipco} section.
     * 
     * @return a list of property Box objects
     */
    public List<Box> getProperties()
    {
        return Collections.unmodifiableList(ipco.properties);
    }

    /**
     * Retrieves the list of item-property associations from the {@code ipma} section.
     * 
     * @return a list of ItemPropertyAssociationBox objects
     */
    public List<ItemPropertyAssociationBox> getAssociations()
    {
        return Collections.unmodifiableList(associations);
    }

    /**
     * Returns a combined list of all boxes contained in this {@code ItemPropertiesBox}, including
     * both properties and associations.
     * 
     * @return a list of Box objects in reading order
     */
    @Override
    public List<Box> addBoxList()
    {
        List<Box> combinedList = new ArrayList<>(ipco.properties);

        combinedList.addAll(associations);

        return combinedList;
    }

    /**
     * Provides a hierarchical string representation of the box structure, including all properties
     * and their associations. Useful for debugging or analysis.
     * 
     * @return a formatted string representing the structure of this box
     */
    @Override
    public String showBoxStructure()
    {
        StringBuilder line = new StringBuilder();

        line.append(String.format("\t%s '%s':%n", this.getClass().getSimpleName(), getBoxName()));
        line.append(String.format("\t\t%s '%s':%n", ipco.getClass().getSimpleName(), ipco.getBoxName()));

        for (Box box : ipco.properties)
        {
            if (HeifBoxType.getBoxType(box.getBoxName()) != HeifBoxType.UNKNOWN)
            {
                line.append(String.format("\t\t\t'%s': %s%n", box.getBoxName(), box.showBoxStructure()));
            }

            else
            {
                line.append(String.format("%s%n", box.showBoxStructure()));
            }
        }

        return line.toString();
    }

    /**
     * Generates a string representation of the basic Box structure.
     *
     * @return a formatted string
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(super.toString());
        line.append(System.lineSeparator());

        for (Box box : ipco.properties)
        {
            line.append(box);
            line.append(System.lineSeparator());
        }

        for (Box box : associations)
        {
            line.append(box);
            line.append(System.lineSeparator());
        }

        return line.toString();
    }
}