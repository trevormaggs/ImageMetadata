package common;

import java.io.IOException;
import java.nio.file.Path;
import heif.HeifParser;
import jpg.JpgParser;
import jpg.JpgParserAdvanced;
import png.PngParser;
import tif.TifParser;
import webp.WebpParser;

/**
 * A static factory class responsible for returning appropriate image parser instances based on the
 * detected image format from file signature bytes.
 *
 * <p>
 * Supported formats include:
 * </p>
 * 
 * <ul>
 * <li>JPEG (JPG)</li>
 * <li>PNG</li>
 * <li>TIFF</li>
 * <li>HEIC (High Efficiency Image Format)</li>
 * <li>WebP</li>
 * </ul>
 *
 * <p>
 * This factory encapsulates format detection logic via {@link DigitalSignature} and delegates
 * instantiation of the correct parser implementation.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public final class ImageParserFactory
{
    /**
     * Prevents direct instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that direct instantiation is not supported
     */
    private ImageParserFactory()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    /**
     * Creates a parser instance for the specified image file by detecting its format.
     *
     * @param fpath
     *        the file path of the image to be parsed
     * 
     * @return a concrete implementation of {@link AbstractImageParser}
     * 
     * @throws IOException
     *         if an I/O error occurs while reading the file signature
     * @throws UnsupportedOperationException
     *         if the format is unsupported
     */
    public static AbstractImageParser getParser(Path fpath) throws IOException
    {
        switch (DigitalSignature.detectFormat(fpath))
        {
            case JPG:
                // return new JpgParser(fpath);
                return new JpgParserAdvanced(fpath);
            // return new JpgParserMultiSegmentTest(fpath);
            case TIF:
                return new TifParser(fpath);
            case PNG:
                return new PngParser(fpath);
            case HEIF:
                return new HeifParser(fpath);
            case WEBP:
                return new WebpParser(fpath);
            default:
                throw new UnsupportedOperationException("Unsupported image format detected [" + fpath.getFileName() + "]");
        }
    }
}