package batch;

import static tif.TagEntries.TagEXIF.EXIF_TAG_CREATE_DATE;
import static tif.TagEntries.TagEXIF.EXIF_TAG_DATE_TIME_ORIGINAL;
import static tif.TagEntries.TagEXIF.EXIF_TAG_METERING_MODE;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import common.AbstractImageParser;
import common.DigitalSignature;
import common.ImageParserFactory;
import common.ImageReadErrorException;
import common.Metadata;
import logger.LogFactory;
import png.ChunkDirectory;
import png.MetadataPNG;
import png.TextEntry;
import png.TextKeyword;
import tif.DirectoryIFD;
import tif.DirectoryIdentifier;
import tif.MetadataTIF;

/**
 * Obtains metadata from selected supported image file formats.
 *
 * At the time of its development, this class supports the following image formats. During reading,
 * the format is determined from the first few bytes of the file.
 *
 * <ul>
 * <li>JPEG files</li>
 * <li>PNG files</li>
 * <li>TIFF files</li>
 * <li>HEIC files</li>
 * <li>WebP (in pipeline)</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public final class TestScanner
{
    private static final LogFactory LOGGER = LogFactory.getLogger(TestScanner.class);
    private AbstractImageParser parser;

    private TestScanner()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    private TestScanner(Path fpath) throws IOException
    {
        parser = ImageParserFactory.getParser(fpath);
    }

    public Metadata<?> readMetadata() throws ImageReadErrorException, IOException
    {
        return parser.readMetadata();
    }

    public Metadata<?> getMetadataInfo() throws ImageReadErrorException
    {
        return parser.getSafeMetadata();
    }

    public Path getFile()
    {
        return parser.getImageFile();
    }

    public DigitalSignature getImageFormat()
    {
        return parser.getImageFormat();
    }

    /* Public static methods */

    public static TestScanner loadImage(Path fpath) throws IOException
    {
        return new TestScanner(fpath);
    }

    public static TestScanner loadImage(String file) throws IOException
    {
        return loadImage(Paths.get(file));
    }

    public static void main(String[] args)
    {
        try
        {
            // TestScanner scanner = TestScanner.loadImage("img\\gemmapreg.tif");
            // TestScanner scanner = TestScanner.loadImage("img\\testPNGimage.png");
            // TestScanner scanner = TestScanner.loadImage("img\\pool19.jpg");
            // TestScanner scanner = TestScanner.loadImage("img\\IMG_0831.HEIC");
            TestScanner scanner = TestScanner.loadImage("img\\SandroBotticelli.webp");

            Metadata<?> meta = scanner.readMetadata();

            if (meta.hasMetadata())
            {
                if (meta instanceof MetadataTIF)
                {
                    MetadataTIF tif = (MetadataTIF) meta;
                    DirectoryIFD dir = tif.getDirectory(DirectoryIdentifier.EXIF_DIRECTORY_SUBIFD);

                    System.out.printf("%s\n", tif.toString("TIFF METADATA SUMMARY LIST"));

                    System.out.printf("dir date %s\n", dir.getString(EXIF_TAG_DATE_TIME_ORIGINAL));
                    System.out.printf("EXIF_DIRECTORY_SUBIFD - %s\n", tif.hasExifData());
                    System.out.printf("EXIF_TAG_CREATE_DATE %s\n", dir.getDate(EXIF_TAG_CREATE_DATE));

                    if (dir.contains(EXIF_TAG_METERING_MODE) && dir.isTagNumeric(EXIF_TAG_METERING_MODE))
                    {
                        System.out.printf("%s\n", dir.getNumericValue(EXIF_TAG_METERING_MODE));
                    }
                }

                else if (meta instanceof MetadataPNG)
                {
                    MetadataPNG<?> png = (MetadataPNG<?>) meta;

                    // System.out.printf("%s\n", png);

                    if (png.hasTextualData())
                    {
                        ChunkDirectory dir = (ChunkDirectory) png.getDirectory(TextKeyword.CREATE);
                        List<TextEntry> keyword = dir.getTextualData(TextKeyword.CREATE);

                        //System.out.printf("%s\n", png.toString("PNG METADATA SUMMARY LIST"));

                        for (TextEntry element : keyword)
                        {
                            System.out.printf("Textual %s\n", element.getValue());
                        }
                    }

                    if (png.hasExifData())
                    {
                        MetadataTIF dir = (MetadataTIF) png.getDirectory(MetadataTIF.class);
                        // System.out.printf("look2 %s\n", dir.toDebugString());
                    }
                }
            }

            else
            {
                System.out.printf("Metadata cannot be found [%s]%n", scanner.getFile());
            }
        }

        catch (ImageReadErrorException | IOException exc)
        {
            LOGGER.error(exc.getMessage());

            System.err.printf("%s\n", exc.getMessage());
            // exc.printStackTrace();
        }
    }
}