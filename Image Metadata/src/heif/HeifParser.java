package heif;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.ImageReadErrorException;
import common.Metadata;
import common.SequentialByteReader;
import heif.boxes.Box;
import logger.LogFactory;
import tif.MetadataTIF;
import tif.TifParser;

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
    private BoxHandler handler;

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
     * <p>
     * Only the Exif block is extracted. Other HEIF boxes are parsed but not returned. If no Exif
     * block is present, an exception is thrown.
     * </p>
     *
     * @return the extracted Exif metadata wrapped in a {@link Metadata} object
     *
     * @throws IOException
     *         if an I/O error occurs
     * @throws ImageReadErrorException
     *         if no Exif block is found or parsing fails
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException, IOException
    {
        try
        {
            byte[] rawBytes = readAllBytes();

            // Use big-endian byte order as per ISO/IEC 14496-12
            SequentialByteReader heifReader = new SequentialByteReader(rawBytes, ByteOrder.BIG_ENDIAN);

            handler = new BoxHandler(getImageFile(), heifReader);
            handler.parseMetadata();

            Optional<byte[]> exif = handler.getExifBlock();

            if (!exif.isPresent())
            {
                throw new ImageReadErrorException("No Exif block found in file [" + getImageFile() + "]");
            }

            metadata = new TifParser(getImageFile(), exif.get()).getMetadata();

            for (Box box : handler)
            {
                // System.out.printf("%s\n", box.getTypeAsString());
                //System.out.printf("%s\n", box.toString(""));
            }

            return metadata;
        }

        catch (IOException exc)
        {
            throw new ImageReadErrorException("Failed to read HEIF file [" + getImageFile() + "]: " + exc.getMessage(), exc);
        }
    }

    /**
     * Retrieves processed metadata from the HEIF image file.
     *
     * @return a populated {@link Metadata} object if present, otherwise an empty object
     */
    @Override
    public Metadata<? extends BaseMetadata> getMetadata()
    {
        if (metadata != null && metadata.hasMetadata())
        {
            return metadata;
        }

        LOGGER.warn("Metadata information could not be found in file [" + getImageFile() + "]");

        return new MetadataTIF();
    }
}