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
 * <b>Example</b>
 * </p>
 * 
 * <pre>
 * AbstractImageParser parser = new JpgParser(Paths.get("image.jpg"));
 * Metadata metadata = parser.readMetadata();
 * </pre>
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
    protected final DigitalSignature format;
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
     * Constructs an image parser and validates the specified image file path.
     * 
     * @param fpath
     *        the path to the image file to be parsed
     * 
     * @throws IllegalArgumentException
     *         if the image format is unsupported
     * @throws IOException
     *         if the file cannot be accessed
     */
    public AbstractImageParser(Path fpath) throws IOException
    {
        DigitalSignature format = DigitalSignature.detectFormat(fpath);

        if (format == DigitalSignature.UNKNOWN)
        {
            throw new IllegalArgumentException("Unsupported image format detected in [" + fpath + "]");
        }

        this.imageFile = fpath;
        this.format = format;
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
     * Returns the extension of the image file name, excluding the dot.
     *
     * <p>
     * If the file name does not contain an extension, an empty string is returned.
     * </p>
     *
     * @return the file extension, for example: {@code "jpg"} or {@code "png"} etc, or an empty
     *         string if none
     */
    protected String checkFileExtension()
    {
        String filename = imageFile.getFileName().toString();
        int pos = filename.lastIndexOf('.');

        if (pos > 0 && pos < filename.length() - 1)
        {
            return filename.substring(pos + 1);
        }

        return "";
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
        byte[] b = Files.readAllBytes(imageFile);

        return (b.length > 0 ? b : new byte[0]);
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
    public abstract Metadata<? extends BaseMetadata> getMetadata();

    /**
     * Returns the detected image format, such as {@code TIFF}, {@code PNG}, or {@code JPG}.
     *
     * @return a {@link DigitalSignature} enum constant representing the image format
     */
    public abstract DigitalSignature getImageFormat();
}