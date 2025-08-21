package common;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import batch.BatchMetadataUtils;

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
    protected static final String FMT = "%-20s:\t%s%n";
    protected static final String DIVIDER = "--------------------------------------------------";
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
     * Produces a human-readable debug string summarising the basic file attributes. Useful for
     * logging or diagnostic output.
     *
     * @return A formatted string containing file path, creation time, last access time, last
     *         modified time, and image format
     * 
     * @throws IOException
     *         if a file system error occurs while reading the attributes
     */
    public String formatDiagnosticString()
    {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

        sb.append("File Attributes").append(System.lineSeparator());
        sb.append(DIVIDER).append(System.lineSeparator());

        try
        {
            BasicFileAttributes attr = BatchMetadataUtils.getFileAttributeView(getImageFile()).readAttributes();

            sb.append(String.format(FMT, "File", getImageFile()));
            sb.append(String.format(FMT, "Creation Time", df.format(new Date(attr.creationTime().toMillis()))));
            sb.append(String.format(FMT, "Last Access Time", df.format(new Date(attr.lastAccessTime().toMillis()))));
            sb.append(String.format(FMT, "Last Modified Time", df.format(new Date(attr.lastModifiedTime().toMillis()))));
            sb.append(String.format(FMT, "Image Format Type", getImageFormat().getFileExtensionName()));
            sb.append(System.lineSeparator());
        }

        catch (IOException exc)
        {
            sb.append("Unable to read file attributes: ").append(exc.getMessage());
            sb.append(System.lineSeparator());
        }

        return sb.toString();
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