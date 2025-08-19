package heif;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.Optional;
import batch.BatchMetadataUtils;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.DigitalSignature;
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
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class HeifParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(HeifParser.class);
    public static final ByteOrder HEIF_BYTE_ORDER = ByteOrder.BIG_ENDIAN;
    private BoxHandler handler;

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

        LOGGER.info("Image file [" + getImageFile() + "] loaded");

        String ext = BatchMetadataUtils.getFileExtension(getImageFile());

        if (!ext.equalsIgnoreCase("heic"))
        {
            LOGGER.warn("File [" + getImageFile().getFileName() + "] has an incorrect extension name. Found [" + ext + "], updating to [heic]");
        }
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
     * Displays the output of each box for diagnostic purposes.
     */
    public void displayDiagnosticOutput()
    {
        LOGGER.debug("Box hierarchy:");

        for (Box box : handler)
        {
            // System.out.printf("%s", box.toString(null));
            LOGGER.debug(String.format("%s", box.toString(null)));
        }
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
     *         in case of processing errors
     * @throws IOException
     *         if the file is not in HEIF format
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException, IOException
    {
        if (metadata == null)
        {
            if (DigitalSignature.detectFormat(getImageFile()) != DigitalSignature.HEIF)
            {
                throw new ImageReadErrorException("Invalid HEIF signature detected in file [" + getImageFile() + "]");
            }

            try
            {
                byte[] bytes = Objects.requireNonNull(readAllBytes(), "Input bytes are null");

                // Use big-endian byte order as per ISO/IEC 14496-12
                SequentialByteReader heifReader = new SequentialByteReader(bytes, HEIF_BYTE_ORDER);

                handler = new BoxHandler(getImageFile(), heifReader);
                handler.parseMetadata();

                Optional<byte[]> exif = handler.getExifData();

                if (!exif.isPresent())
                {
                    LOGGER.warn("No Exif block found in file [" + getImageFile() + "]");

                    /* Fallback to empty metadata */
                    metadata = new MetadataTIF();
                }

                else
                {
                    metadata = new TifParser(getImageFile(), exif.get()).getSafeMetadata();
                }
            }

            catch (IOException exc)
            {
                throw new ImageReadErrorException("Failed to read HEIF file [" + getImageFile() + "]", exc);
            }
        }

        // handler.displayHierarchy();
        // displayDiagnosticOutput();

        return metadata;
    }

    /**
     * Retrieves processed metadata from the HEIF image file.
     *
     * @return a populated {@link Metadata} object if present, otherwise an empty object
     */
    @Override
    public Metadata<? extends BaseMetadata> getSafeMetadata()
    {
        if (metadata == null)
        {
            LOGGER.warn("Metadata information has not been parsed yet.");

            return new MetadataTIF();
        }

        return metadata;
    }

    /**
     * Returns the detected {@code HEIF} format.
     *
     * @return a {@link DigitalSignature} enum constant representing this image format
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.HEIF;
    }
    
    /**
     * Prints diagnostic information including file attributes and metadata content.
     *
     * @param prefix
     *        optional label or heading, can be null
     *
     * @return formatted string suitable for diagnostics
     */
    @Override
    public String toString(String prefix)
    {
        String fmt = "%-20s:\t%s%n";
        String divider = "--------------------------------------------------";
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        
        return sb.toString();
    }
}