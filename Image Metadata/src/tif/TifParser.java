package tif;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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

public class TifParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TifParser.class);

    /**
     * This default constructor should not be invoked, or it will throw an exception to prevent
     * instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    public TifParser()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * This constructor creates an instance intended for parsing the specified TIFF image file.
     *
     * @param fpath
     *        specifies the TIFF file path, encapsulated as a Path object
     *
     * @throws IOException
     *         if an I/O problem has occurred
     */
    public TifParser(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info(String.format("Image file [%s] loaded for reading", getImageFile()));
    }

    /**
     * This constructor creates an instance intended for parsing the specified TIFF image file.
     *
     * @param file
     *        specifies the TIFF image file
     *
     * @throws IOException
     *         if an I/O problem has occurred
     */
    public TifParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Constructs an instance used for reading the payload data contained in the specified
     * SequentialByteReader object, providing read access. These data elements are mapped to Image
     * File Directory (IFD) structures.
     *
     * @param fpath
     *        specifies the file path, encapsulated as a Path object
     * @param reader
     *        a SequentialByteReader object containing the segment data
     *
     * @throws IOException
     *         In case of an I/O issue.
     */
    public TifParser(Path fpath, SequentialByteReader reader) throws IOException
    {
        this(fpath);

        processDirectories(reader);
    }

    /**
     * This constructor creates an instance used for parsing the payload data representing the Image
     * File Directory (IFD) structures.
     *
     * <p>
     * <b>Important Note:</b> Use this constructor when handling JPG image files, as they support
     * IFD structures too.
     * </p>
     *
     * @param fpath
     *        specifies the file path, encapsulated as a Path object
     * @param payload
     *        an array of bytes containing raw data within the Exif segment block
     *
     * @throws IOException
     *         if an I/O problem has occurred
     */
    public TifParser(Path fpath, byte[] payload) throws IOException
    {
        this(fpath);

        processDirectories(new SequentialByteReader(payload));
    }

    /**
     * Reads and processes a TIFF image file, returning a new Metadata object. Throws exceptions for
     * any processing issues or non-TIFF formats are detected.
     *
     * @return a Metadata object containing extracted metadata
     * @throws IOException
     *         if the file is not in TIFF format
     * @throws ImageReadErrorException
     *         in case of processing errors
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException, IOException
    {
        if (DigitalSignature.detectFormat(getImageFile()) == DigitalSignature.TIF)
        {
            try (InputStream fis = Files.newInputStream(getImageFile()))
            {
                SequentialByteReader tifReader = new SequentialByteReader(readAllBytes());
                processDirectories(tifReader);

                // tifReader.printRawBytes();
            }

            catch (NoSuchFileException exc)
            {
                throw new ImageReadErrorException("File [" + getImageFile() + "] does not exist", exc);
            }

            catch (IOException exc)
            {
                throw new ImageReadErrorException("Problem encountered while reading the stream from file [" + getImageFile() + "]", exc);
            }
        }

        else
        {
            throw new ImageReadErrorException("Image file [" + getImageFile() + "] is not a TIFF type");
        }

        return getMetadata();
    }

    /**
     * Retrieves processed metadata from the TIFF image file.
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

        /* Fallback to empty metadata */
        return new MetadataTIF();
    }

    /**
     * Starts the processing of the IFD structures, handling the required information until all
     * directories have been read. It also ensures the TIFF segment data is checked beforehand for
     * integrity.
     *
     * @param reader
     *        a SequentialByteReader object providing access to the payload data
     */
    private void processDirectories(SequentialByteReader reader)
    {
        MetadataTIF tif = new MetadataTIF();
        IFDHandler handler = new IFDHandler(reader);

        handler.parseMetadata();

        Optional<List<DirectoryIFD>> optionalData = handler.getDirectories();

        if (optionalData.isPresent())
        {
            for (DirectoryIFD dir : optionalData.get())
            {
                tif.addDirectory(dir);
            }

            metadata = tif;
        }
    }
}