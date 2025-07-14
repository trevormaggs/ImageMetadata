package common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An abstract superclass for implementing image file parsers. Subclasses are responsible for
 * decoding specific image formats (for example, JPEG, PNG, TIFF) and extracting metadata
 * structures.
 *
 * <p>
 * This class provides basic file loading and byte-reading utilities, and defines a contract for
 * reading metadata through abstract methods.
 * </p>
 *
 * <p>
 * <strong>Usage:</strong>
 * </p>
 * 
 * <ul>
 * <li>Subclass this to support format-specific parsing</li>
 * <li>Use {@link #readMetadata()} to trigger extraction</li>
 * </ul>
 *
 * <p>
 * <strong>Change History:</strong>
 * </p>
 * 
 * <ul>
 * <li>Version 1.0 – Initial release by Trevor Maggs on 9 July 2025</li>
 * </ul>
 *
 * @version 1.0
 * @since 9 July 2025
 * @author Trevor Maggs
 */
public abstract class AbstractImageParser
{
    private final Path imageFile;
    protected Metadata<? extends BaseMetadata> metadata;

    /**
     * Prevents direct instantiation without a file path.
     *
     * @throws UnsupportedOperationException
     *         to indicate that direct instantiation is not supported
     */
    public AbstractImageParser()
    {
        throw new UnsupportedOperationException("Instantiation not allowed");
    }

    /**
     * Creates this parser and validates the specified image file path.
     * 
     * @param fpath
     *        the file path of the image to parse
     * 
     * @throws IOException
     *         if the image cannot be accessed or is unsupported
     */
    public AbstractImageParser(Path fpath) throws IOException
    {
        if (DigitalSignature.detectFormat(fpath) == DigitalSignature.UNKNOWN)
        {
            throw new IllegalArgumentException("Unsupported image format detected in [" + fpath + "]");
        }

        this.imageFile = fpath;
    }

    /**
     * Gets the image file path used for parsing.
     *
     * @return the image file {@link Path}
     */
    protected Path getImageFile()
    {
        return imageFile;
    }

    /**
     * Reads the entire contents of the image file into a byte array.
     *
     * @return the file's raw byte data
     * @throws IOException
     *         if the file cannot be read
     */
    protected byte[] readAllBytes() throws IOException
    {
        return Files.readAllBytes(imageFile);
    }

    /**
     * Reads and extracts metadata from the image file.
     *
     * @return a {@link Metadata} container with parsed metadata
     * 
     * @throws ImageReadErrorException
     *         if a parsing error occurs
     * @throws IOException
     *         if the file cannot be read
     */
    public abstract Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException, IOException;

    /**
     * Returns parsed metadata, or throws if {@link #readMetadata()} has not been called.
     *
     * @return parsed {@link Metadata}
     * 
     * @throws ImageReadErrorException
     *         if a parsing error occurs
     */
    public abstract Metadata<? extends BaseMetadata> getMetadata() throws ImageReadErrorException;
}