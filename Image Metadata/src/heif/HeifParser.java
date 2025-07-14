package heif;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.ImageReadErrorException;
import common.Metadata;
import common.SequentialByteReader;
import heif.boxes.Box;
import logger.LogFactory;

/**
 * Parses HEIF/HEIC image files and extracts embedded metadata.
 *
 * HEIF files are based on the ISO Base Media File Format (ISOBMFF). This parser extracts Exif
 * metadata by navigating the box structure defined in {@code ISO/IEC 14496-12} and
 * {@code ISO/IEC 23008-12} documents.
 *
 * <ul>
 * <li>Created by Trevor Maggs on 11 July 2025</li>
 * </ul>
 */
public class HeifParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(HeifParser.class);

    /**
     * This default constructor should not be invoked.
     *
     * @throws UnsupportedOperationException
     *         to prevent instantiation without a file.
     */
    public HeifParser()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Constructs an instance to parse a HEIC/HEIF file.
     *
     * @param fpath
     *        the image file path
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public HeifParser(Path fpath) throws IOException
    {
        super(fpath);
        LOGGER.info(String.format("Image file [%s] loaded for reading%n", getImageFile()));
    }

    /**
     * Constructs an instance to parse a HEIC/HEIF file.
     *
     * @param file
     *        the image file path as a string
     * 
     * @throws IOException
     *         if an I/O error occurs
     */
    public HeifParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Reads and processes Exif metadata from the HEIC/HEIF file.
     *
     * @return the extracted metadata
     * 
     * @throws IOException
     *         if the file is not a HEIC/HEIF format
     * @throws ImageReadErrorException
     *         if parsing fails
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException, IOException
    {
        SequentialByteReader heifReader;

        try
        {
            byte[] rawBytes = readAllBytes();

            // Use big-endian byte order as per ISO/IEC 14496-12
            heifReader = new SequentialByteReader(rawBytes, ByteOrder.BIG_ENDIAN);
        }

        catch (NoSuchFileException exc)
        {
            throw new ImageReadErrorException("File [" + getImageFile() + "] does not exist", exc);
        }

        catch (IOException exc)
        {
            throw new ImageReadErrorException(exc);
        }

        BoxHandler handler = new BoxHandler(getImageFile(), heifReader);

        metadata = handler.processMetadata();

        Map<HeifBoxType, List<Box>> map = handler.getBoxes();

        for (List<Box> list : map.values())
        {
            for (Box box : list)
            {
                System.out.printf("%s\n", box.showBoxStructure());
            }
        }

        return metadata;
    }

    /**
     * Returns the previously extracted metadata.
     *
     * @return the populated metadata
     * 
     * @throws ImageReadErrorException
     *         if metadata was not extracted
     */
    @Override
    public Metadata<? extends BaseMetadata> getMetadata() throws ImageReadErrorException
    {
        if (metadata != null && metadata.hasMetadata())
        {
            return metadata;
        }

        throw new ImageReadErrorException("Metadata could not be found in file [" + getImageFile() + "]");
    }
}