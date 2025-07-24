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
     * This method extracts only the Exif segment from the file. While other HEIF boxes are parsed
     * internally, they are not returned or exposed.
     * </p>
     *
     * @return the extracted Exif metadata wrapped in a {@link Metadata} object, if no Exif data is
     *         found, returns an empty metadata instance
     *
     * @throws ImageReadErrorException
     *         if an error occurs while parsing the file
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException
    {
        if (metadata == null)
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
                    LOGGER.warn("No Exif block found in file [" + getImageFile() + "]");

                    /* Fallback to empty metadata */
                    return new MetadataTIF();
                }

                metadata = new TifParser(getImageFile(), exif.get()).getMetadata();
            }

            catch (IOException exc)
            {
                throw new ImageReadErrorException("Failed to read HEIF file [" + getImageFile() + "]", exc);
            }

            for (Box box : handler)
            {
                //System.out.printf("%s\n", box.getTypeAsString());
                System.out.printf("%s\n", box.toString(""));
            }
        }

        return metadata;
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

        LOGGER.warn("Metadata information is not available in file [" + getImageFile() + "]");

        return new MetadataTIF();
    }
}