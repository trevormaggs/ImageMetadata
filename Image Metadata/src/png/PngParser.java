package png;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import batch.BatchMetadataUtils;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.DigitalSignature;
import common.ImageFileInputStream;
import common.ImageReadErrorException;
import common.Metadata;
import logger.LogFactory;
import png.ChunkType.Category;
import tif.DirectoryIFD;
import tif.DirectoryIFD.EntryIFD;
import tif.MetadataTIF;
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

        String ext = BatchMetadataUtils.getFileExtension(getImageFile());

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
        Metadata<BaseMetadata> png = new MetadataPNG<>();

        try (ImageFileInputStream pngReader = new ImageFileInputStream(getImageFile(), PNG_BYTE_ORDER))
        {
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
            throw new ImageReadErrorException("Problem reading data stream: [" + exc.getMessage() + "]", exc);
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

        if (prefix != null)
        {
            sb.append(prefix).append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }

        try
        {
            Metadata<?> meta = getSafeMetadata();

            sb.append("File Attributes").append(System.lineSeparator());
            sb.append(divider).append(System.lineSeparator());

            try
            {
                BasicFileAttributeView attr = BatchMetadataUtils.getFileAttributeView(getImageFile());

                sb.append(String.format(fmt, "File", getImageFile()));
                sb.append(String.format(fmt, "Creation Time", df.format(new Date(attr.readAttributes().creationTime().toMillis()))));
                sb.append(String.format(fmt, "Last Access Time", df.format(new Date(attr.readAttributes().lastAccessTime().toMillis()))));
                sb.append(String.format(fmt, "Last Modified Time", df.format(new Date(attr.readAttributes().lastModifiedTime().toMillis()))));
                sb.append(String.format(fmt, "Image Format Type", getImageFormat().getFileExtensionName()));
            }

            catch (IOException exc)
            {
                sb.append("Unable to read file attributes: ").append(exc.getMessage());
                sb.append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());

            if (meta.hasMetadata())
            {
                if (meta instanceof MetadataPNG<?>)
                {
                    MetadataPNG<?> png = (MetadataPNG<?>) meta;

                    if (png.hasTextualData())
                    {
                        for (Object obj : png)
                        {
                            if (obj instanceof ChunkDirectory)
                            {
                                ChunkDirectory dir = (ChunkDirectory) obj;

                                if (dir.getDirectoryCategory() == Category.TEXTUAL)
                                {
                                    sb.append("Textual Chunks").append(System.lineSeparator());
                                    sb.append(divider).append(System.lineSeparator());

                                    for (PngChunk chunk : dir)
                                    {
                                        String keywordValue = (chunk.getKeywordPair().isPresent() ? chunk.getKeywordPair().get().getKeyword() : "N/A");
                                        String textValue = (chunk.getKeywordPair().isPresent() ? chunk.getKeywordPair().get().getValue() : "N/A");

                                        sb.append(String.format(fmt, "Tag Type", chunk.getTag()));
                                        sb.append(String.format(fmt, "Chunk Type", chunk.getType()));
                                        sb.append(String.format(fmt, "Chunk Bytes", chunk.getLength()));
                                        sb.append(String.format(fmt, "Keyword", keywordValue));
                                        sb.append(String.format(fmt, "Text", textValue));
                                        sb.append(System.lineSeparator());
                                    }

                                    break;
                                }
                            }
                        }
                    }

                    else
                    {
                        sb.append("No textual metadata found.").append(System.lineSeparator());
                    }

                    if (png.hasExifData())
                    {
                        Object obj = png.getDirectory(MetadataTIF.class);

                        if (obj instanceof MetadataTIF)
                        {
                            MetadataTIF exifDir = (MetadataTIF) obj;

                            sb.append("EXIF Metadata").append(System.lineSeparator());
                            sb.append(divider).append(System.lineSeparator());

                            for (DirectoryIFD ifd : exifDir)
                            {
                                sb.append("\tDirectory - ").append(ifd.getDirectoryType().getDescription());
                                sb.append(System.lineSeparator()).append(System.lineSeparator());

                                for (EntryIFD entry : ifd)
                                {
                                    sb.append(String.format(fmt, "Tag Type", entry.getTag()));
                                    sb.append(String.format("%-20s:\t0x%04X%n", "Tag ID", entry.getTagID()));
                                    sb.append(String.format(fmt, "Field Type", entry.getFieldType()));
                                    sb.append(String.format(fmt, "Count", entry.getCount()));
                                    sb.append(String.format(fmt, "Value", ifd.getStringValue(entry)));
                                    sb.append(System.lineSeparator());
                                }
                            }
                        }
                    }

                    else
                    {
                        sb.append("No EXIF metadata found.").append(System.lineSeparator());
                    }
                }
            }

            else
            {
                sb.append("No metadata found...").append(System.lineSeparator());
            }

            sb.append(System.lineSeparator());
        }

        catch (Exception exc)
        {
            LOGGER.error("Diagnostics failed for file [" + getImageFile() + "]", exc);
        }

        return sb.toString();
    }
}