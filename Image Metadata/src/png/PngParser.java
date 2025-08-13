package png;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.DigitalSignature;
import common.ImageFileInputStream;
import common.ImageReadErrorException;
import common.Metadata;
import logger.LogFactory;
import png.ChunkType.Category;
import tif.TifParser;

/**
 * This program aims to read PNG image files and retrieve data structured in a series of chunks. For
 * accessing metadata, only any of the textual chunks or the EXIF chunk, if present, will be
 * processed.
 *
 * Normally, most PNG files do not contain the EXIF structure, however, it will attempt to search
 * for these 4 potential chunks: ChunkType.eXIf, ChunkType.tEXt, ChunkType.iTXt or ChunkType.zTXt.
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
 * <b>Critical Chunk Types</b>
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
 * <b>Ancillary Chunk Types</b>
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
 * @see <a href="https://www.w3.org/TR/png">See this link for more technical background
 *      information.</a>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class PngParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngParser.class);
    private static final ByteOrder PNG_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

    /**
     * This constructor creates an instance for processing the specified image file.
     *
     * @param fpath
     *        specifies the PNG file path, encapsulated in a Path object
     *
     * @throws IOException
     *         if an I/O issue arises
     */
    public PngParser(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info("Image file [" + getImageFile() + "] loaded");

        String ext = getFileExtension();

        if (!ext.equalsIgnoreCase("png"))
        {
            LOGGER.warn("File [" + getImageFile().getFileName() + "] has an incorrect extension name. Found [" + ext + "], updating to [png]");
        }
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
    public PngParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Parses data in the PNG image file and returns a new Metadata object. It is important to note
     * that PNG files usually do not have an EXIF segment block structured inside.
     *
     * However, it will attempt to find information from 4 possible chunks:
     * {@code ChunkType.eXIf, ChunkType.tEXt, ChunkType.iTXt or ChunkType.zTXt}. The last 3 chunks
     * are textual.
     *
     * If any of these 3 textual chunks does contain data, it will be quite rudimentary, such as
     * obtaining the Creation Time, Last Modification Date, etc.
     *
     * See https://www.w3.org/TR/png/#11keywords for more information.
     *
     * @return a Metadata object containing extracted metadata
     *
     * @throws ImageReadErrorException
     *         in case of processing errors
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException
    {
        // For full metadata parsing (image properties + text), include IHDR, sRGB, etc.
        EnumSet<ChunkType> chunkSet = EnumSet.of(ChunkType.tEXt, ChunkType.zTXt, ChunkType.iTXt, ChunkType.eXIf);

        try (InputStream fis = Files.newInputStream(getImageFile()))
        {
            // Use big-endian byte order as per Specifications
            ImageFileInputStream pngReader = new ImageFileInputStream(fis, PNG_BYTE_ORDER);
            Metadata<BaseMetadata> png = new MetadataPNG<>();
            ChunkHandler handler = new ChunkHandler(getImageFile(), pngReader, chunkSet);

            handler.parseMetadata();

            // Obtain textual information if present
            Optional<List<PngChunk>> textual = handler.getTextualData();

            if (textual.isPresent())
            {
                ChunkDirectory textualDir = new ChunkDirectory(Category.TEXTUAL);

                for (PngChunk chunk : textual.get())
                {
                    textualDir.add(chunk);
                }

                png.addDirectory(textualDir);
            }

            else
            {
                LOGGER.info("No textual information found in file [" + getImageFile() + "]");
            }

            // Obtain Exif information if present
            Optional<byte[]> exif = handler.getExifData();

            if (exif.isPresent())
            {
                png.addDirectory(TifParser.parseFromSegmentBytes(exif.get()));
            }

            else
            {
                LOGGER.info("No Exif block found in file [" + getImageFile() + "]");
            }

            metadata = png;
        }

        catch (NoSuchFileException exc)
        {
            throw new ImageReadErrorException("File [" + getImageFile() + "] does not exist", exc);
        }

        catch (IOException exc)
        {
            throw new ImageReadErrorException("Problem while reading the stream in file [" + getImageFile() + "]", exc);
        }

        return metadata;
    }

    /**
     * Retrieves previously parsed metadata from the PNG file.
     *
     * @return a populated {@link Metadata} object, or an empty one if no metadata was found
     */
    @Override
    public Metadata<? extends BaseMetadata> getSafeMetadata()
    {
        if (metadata == null)
        {
            LOGGER.warn("Metadata information has not been parsed yet.");

            return new MetadataPNG<>();
        }

        return metadata;
    }

    /**
     * Returns the detected {@code PNG} format.
     *
     * @return a {@link DigitalSignature} enum constant representing this image format
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.PNG;
    }
}