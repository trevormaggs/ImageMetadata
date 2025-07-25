package common;

import java.io.IOException;
import java.nio.file.Path;
import heif.HeifParser;
import jpg.JpgParser;
import png.PngParser;
import tif.TifParser;

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
 * <li>WebP (planned, not yet implemented)</li>
 * </ul>
 *
 * <p>
 * This factory encapsulates format detection logic via {@link DigitalSignature} and delegates
 * instantiation of the correct parser implementation.
 * </p>
 *
 * <p>
 * <strong>Change History:</strong>
 * </p>
 * <ul>
 * <li>Version 1.0 – Initial release by Trevor Maggs on 9 July 2025</li>
 * </ul>
 *
 * @version 1.0
 * @since 9 July 2025
 * @author Trevor Maggs
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
     * @throws IOException
     *         if an I/O error occurs while reading the file signature
     * @throws UnsupportedOperationException
     *         if the image format is not supported
     */
    private static AbstractImageParser createParser(Path fpath) throws IOException
    {
        switch (DigitalSignature.detectFormat(fpath))
        {
            case JPG:
                return new JpgParser(fpath);
            case TIF:
                return new TifParser(fpath);
            case PNG:
                return new PngParser(fpath);
            case HEIF:
                return new HeifParser(fpath);
            case WEBP:
                throw new UnsupportedOperationException("WebP support not implemented yet");
            default:
                throw new UnsupportedOperationException("Unsupported image format detected [" + fpath.getFileName() + "]");
        }
    }

    /**
     * A public static entry point to fetch the appropriate parser for the specified image file.
     *
     * @param fpath
     *        the path to the image file
     * 
     * @return an image parser capable of reading the image's format
     * @throws IOException
     *         if an I/O error occurs or the format is unsupported
     * @throws UnsupportedOperationException
     *         if the format is unsupported
     */
    public static AbstractImageParser getParser(Path fpath) throws IOException
    {
        return createParser(fpath);
    }
}