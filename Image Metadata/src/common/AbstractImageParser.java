package common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An abstract superclass for implementing image file parsers. Subclasses are responsible for
 * decoding specific image formats, for example: JPEG, PNG, TIFF, etc and extracting metadata
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
 * <b>Example</b>
 * </p>
 * 
 * <pre>
 * AbstractImageParser parser = new JpgParser(Paths.get("image.jpg"));
 * Metadata metadata = parser.readMetadata();
 * </pre>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public abstract class AbstractImageParser
{
    private final Path imageFile;
    protected Metadata<? extends BaseMetadata> metadata;

    /**
     * Constructs an image parser.
     * 
     * @param fpath
     *        the path to the image file to be parsed
     * 
     * @throws IllegalStateException
     *         if the specified file is null
     */
    public AbstractImageParser(Path fpath)
    {
        if (fpath == null)
        {
            throw new IllegalStateException("Image file cannot be null");
        }

        this.imageFile = fpath;
    }

    /**
     * Gets the image file path used for parsing.
     *
     * @return the image file {@link Path}
     */
    public Path getImageFile()
    {
        return imageFile;
    }

    /**
     * Reads the entire contents of the image file into a byte array.
     *
     * @return a non-null byte array of the file's raw contents, or empty if file is zero-length
     *
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
     * Returns the extracted metadata, if available.
     *
     * @return a populated {@link Metadata} object if parsing was successful, otherwise an empty
     *         container
     */
    public abstract Metadata<? extends BaseMetadata> getSafeMetadata();

    /**
     * Returns the detected image format, such as {@code TIFF}, {@code PNG}, or {@code JPG}.
     *
     * @return a {@link DigitalSignature} enum constant representing the image format
     */
    public abstract DigitalSignature getImageFormat();
}