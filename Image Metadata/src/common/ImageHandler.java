package common;

import java.io.IOException;

/**
 * Defines the contract for a handler that is responsible for processing image files and extracting
 * metadata structures. Implementations of this interface are expected to parse binary image data
 * and produce structured metadata in the form of a {@link Metadata}.
 *
 * <p>
 * This abstraction allows for extensibility to support different image formats (For example, TIFF,
 * PNG, JPEG), each with its own parsing and metadata representation logic.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public interface ImageHandler
{
    /**
     * Returns the length of the image file associated with the current InputStream resource.
     * 
     * <p>
     * Any {@link IOException} that occurs while determining the size will be handled internally,
     * and the method will return {@code 0} if the size cannot be determined.
     * </p>
     *
     * @return the length of the file in bytes, or 0 if the size cannot be determined
     */
    public long getSafeFileLength();

    /**
     * Parses the image data stream and attempts to extract metadata.
     * 
     * <p>
     * Implementations should read the appropriate sections of the image file and collect any
     * available metadata into their internal structures.
     * </p>
     *
     * @return true if metadata was successfully extracted, otherwise false
     *
     * @throws ImageReadErrorException
     *         if the file format is invalid or the data cannot be interpreted as valid metadata
     * @throws IOException
     *         if there is a low-level I/O problem reading the image file
     */
    public boolean parseMetadata() throws ImageReadErrorException, IOException;
}