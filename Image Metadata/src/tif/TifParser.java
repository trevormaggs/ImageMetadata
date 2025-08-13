package tif;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.DigitalSignature;
import common.ImageReadErrorException;
import common.Metadata;
import common.SequentialByteReader;
import logger.LogFactory;

/**
 * This program aims to read TIF image files and retrieve data structured in a series of Image File
 * Directories (IFDs).
 *
 * <p>
 * <b>TIF Data Stream</b>
 * </p>
 *
 * <p>
 * The TIF data stream begins with an 8-byte header, which specifies the byte order (either
 * big-endian "II" or little-endian "MM"), a fixed identifier (0x002A), and the offset to the first
 * IFD. The file is composed of one or more IFDs, each containing a series of tags that define the
 * image's characteristics and metadata.
 * </p>
 *
 * <p>
 * The TIFF specification is extensible, allowing for custom tags and nested sub-directories, such
 * as the {@code EXIF (Exchangeable Image File Format)}, which is a common sub-directory used in
 * both TIFF and JPEG files.
 * </p>
 *
 * @see <a href="https://partners.adobe.com/public/developer/en/tiff/TIFF6.pdf">TIFF 6.0 Specification</a>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class TifParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TifParser.class);

    /**
     * Creates an instance for parsing the specified TIFF image file.
     *
     * @param file
     *        the path to the TIFF file
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    public TifParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Creates an instance intended for parsing the specified TIFF image file.
     *
     * @param fpath
     *        specifies the TIFF file path, encapsulated as a Path object
     *
     * @throws IOException
     *         if an I/O error occurs
     */
    public TifParser(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info("Image file [" + getImageFile() + "] loaded");

        String ext = getFileExtension();

        if (!ext.equalsIgnoreCase("tif") && !ext.equalsIgnoreCase("tiff"))
        {
            LOGGER.warn("File [" + getImageFile().getFileName() + "] has an incorrect extension name. Found [" + ext + "], updating to [.tif]");
        }
    }

    /**
     * Constructs an instance used for parsing the payload data representing the Image File
     * Directory (IFD) structures.
     *
     * <p>
     * <b>Important Note:</b> Use this constructor when handling JPG image files, as they support
     * IFD structures too.
     * </p>
     *
     * @param fpath
     *        specifies the TIFF file path, encapsulated as a Path object
     * @param payload
     *        byte array containing Exif TIFF data
     *
     * @throws IOException
     *         if an I/O error occurs
     * @throws ImageReadErrorException
     *         if the IFD structures cannot be parsed
     */
    public TifParser(Path fpath, byte[] payload) throws ImageReadErrorException, IOException
    {
        super(fpath);

        try
        {
            metadata = parseFromSegmentBytes(payload);
        }

        catch (IllegalStateException exc)
        {
            throw new ImageReadErrorException("Failed to parse IFD structures from payload", exc);
        }
    }

    /**
     * Reads and processes a TIFF image file.
     *
     * @return metadata extracted from the file
     *
     * @throws IOException
     *         if an I/O error occurs
     * @throws ImageReadErrorException
     *         if the file is not a valid TIFF
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException, IOException
    {
        try
        {
            metadata = parseFromSegmentBytes(readAllBytes());
        }

        catch (IllegalStateException exc)
        {
            throw new ImageReadErrorException("TIFF file appears corrupted or has an invalid structure", exc);
        }

        return getSafeMetadata();
    }

    /**
     * Retrieves the extracted metadata, or a fallback if unavailable.
     *
     * @return a {@link Metadata} object
     */
    @Override
    public Metadata<? extends BaseMetadata> getSafeMetadata()
    {
        if (metadata == null)
        {
            LOGGER.warn("Metadata information has not been parsed yet");
            return new MetadataTIF();
        }

        return metadata;
    }

    /**
     * Returns the detected TIFF format.
     *
     * @return a {@link DigitalSignature} enum
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.TIF;
    }

    /**
     * Parses TIFF metadata from a byte array, providing information on extracted TIFF directories.
     *
     * <p>
     * For efficiency, use this static utility method where TIFF-formatted data, such as an embedded
     * EXIF segment, is already available in memory. It directly processes the byte array to extract
     * and structure the metadata directories without needing to read a file from disk again.
     * </p>
     *
     * <p>
     * Note: This method assumes the provided byte array is a valid TIFF or EXIF payload. No
     * external validation is performed.
     * </p>
     *
     * @param payload
     *        byte array containing TIFF-formatted data
     *
     * @return parsed metadata
     * 
     * @throws IllegalStateException
     *         if the TIFF header is invalid or the stream data cannot be read correctly
     */
    public static MetadataTIF parseFromSegmentBytes(byte[] payload) throws IllegalStateException
    {
        MetadataTIF tif = new MetadataTIF();
        IFDHandler handler = new IFDHandler(new SequentialByteReader(payload));
        handler.parseMetadata();

        Optional<List<DirectoryIFD>> optionalData = handler.getDirectories();

        if (optionalData.isPresent())
        {
            for (DirectoryIFD dir : optionalData.get())
            {
                tif.addDirectory(dir);
            }
        }

        return tif;
    }
}