package webp;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.DigitalSignature;
import common.ImageReadErrorException;
import common.Metadata;
import common.SequentialByteReader;
import logger.LogFactory;

/**
 * This program aims to read WEBP image files and retrieve data structured in a series of chunks.
 * For accessing metadata, only the EXIF chunk, if present, will be processed.
 *
 * <p>
 * <b>PNG Data Stream</b>
 * </p>
 *
 * <p>
 * The PNG data stream begins with a PNG SIGNATURE (0x89 0x50 0x4E 0x47 0x0D 0x0A 0x1A 0x0A)
 * followed by a series of chunks. Each chunk consists of:
 * </p>
 *
 * <ul>
 * <li>4 bytes for data field length (unsigned, usually &lt;= 31 bytes)</li>
 * <li>4 bytes for chunk type (only [65-90] and [97-122]) ASCII codes</li>
 * <li>Variable number of bytes for data field</li>
 * <li>4 bytes for CRC computed from chunk type and data only</li>
 * </ul>
 *
 * <p>
 * There are two categories of chunks: Critical and Ancillary.
 * </p>
 *
 * <p>
 * <b>Mandatory Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>IHDR - image header, always the initial chunk in the data stream</li>
 * <li>PLTE - palette table, relevant for indexed PNG images</li>
 * <li>IDAT - image data chunk, multiple occurrences likely</li>
 * <li>IEND - image trailer, always the final chunk in the data stream</li>
 * </ul>
 *
 * <p>
 * <b>Optional Chunk Types</b>
 * </p>
 *
 * <ul>
 * <li>Transparency info: tRNS</li>
 * <li>Colour space info: cHRM, gAMA, iCCP, sBIT, sRGB</li>
 * <li>Textual info: iTXt, tEXt, zTXt</li>
 * <li>Miscellaneous info: bKGD, hIST, pHYs, sPLT</li>
 * <li>Time info: tIME</li>
 * </ul>
 *
 * <p>
 * <b>Chunk Processing</b>
 * </p>
 *
 * <ul>
 * <li>Only chunks of specified types in the {@code requiredChunks} list are read</li>
 * <li>An empty {@code requiredChunks} list results in no data being extracted from the source
 * stream</li>
 * <li>A null list results in all data being copied from the source stream</li>
 * </ul>
 *
 * <p>
 * Change History:
 * </p>
 *
 * <ul>
 * <li>Version 1.0 - First release by Trevor Maggs on 31 July 2025</li>
 * </ul>
 *
 * @see <a href="https://https://developers.google.com/speed/webp/docs/riff_container">See this link
 *      for more technical background information.</a>
 *
 * @version 1.0
 * @author Trevor Maggs, trevmaggs@tpg.com.au
 * @since 20 July 2025
 */
public class WebpParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(WebpParser.class);
    public static final ByteOrder WEBP_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    /**
     * This default constructor should not be invoked, or it will throw an exception to prevent
     * instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    public WebpParser()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * This constructor creates an instance for processing the specified image file.
     *
     * @param fpath
     *        specifies the PNG file path, encapsulated in a Path object
     *
     * @throws IOException
     *         if an I/O issue arises
     */
    public WebpParser(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info("Image file [" + getImageFile() + "] loaded for parsing");
    }

    /**
     * This constructor creates an instance for processing the specified image file.
     *
     * @param file
     *        specifies the PNG image file to be read
     *
     * @throws IOException
     *         if an I/O problem has occurred
     */
    public WebpParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Parses data in the WEBP image file and returns a new Metadata object.
     *
     * @return a Metadata object containing extracted metadata
     *
     * @throws ImageReadErrorException
     *         in case of processing errors
     * @throws IOException
     *         if the file is not in WEBP format
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException, IOException
    {
        EnumSet<WebPChunkType> chunkSet = EnumSet.of(WebPChunkType.EXIF);

        if (DigitalSignature.detectFormat(getImageFile()) == DigitalSignature.WEBP)
        {
            try
            {
                // Use little-endian byte order as per Specifications
                SequentialByteReader webpReader = new SequentialByteReader(readAllBytes(), WEBP_BYTE_ORDER);

                // webpReader.printRawBytes();

                WebpHandler handler = new WebpHandler(getImageFile(), webpReader, chunkSet);
                handler.parseMetadata();
            }

            catch (NoSuchFileException exc)
            {
                throw new ImageReadErrorException("File [" + getImageFile() + "] does not exist", exc);
            }

            catch (IOException exc)
            {
                throw new ImageReadErrorException("Problem while reading the stream in file [" + getImageFile() + "]", exc);
            }
        }

        else
        {
            throw new ImageReadErrorException("Image file [" + getImageFile() + "] is not a WEBP type");
        }

        return metadata;
    }

    /**
     * Retrieves previously parsed metadata from the WEBP file.
     *
     * @return a populated {@link Metadata} object, or an empty one if no metadata was found
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
        return null;
    }
}