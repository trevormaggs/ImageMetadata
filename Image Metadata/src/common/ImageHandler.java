package common;

import java.io.IOException;

/**
 * Defines the contract for a handler that processes image files and extracts structured metadata.
 * Implementations are responsible for parsing binary image data and producing metadata in the form
 * of a {@link Metadata} instance.
 *
 * <p>
 * This interface allows for extensibility to support different image formats, for example: TIFF,
 * PNG, JPEG, etc, each with its own parsing and metadata representation logic.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public interface ImageHandler
{
    /**
     * Returns the size of the image file being processed, in bytes.
     * 
     * <p>
     * Any {@link IOException} that occurs while determining the size will be handled internally,
     * and the method will return {@code 0} if the size cannot be determined.
     * </p>
     *
     * @return the file size in bytes, or 0 if it cannot be determined
     */
    long getSafeFileSize();

    /**
     * Parses the image data and attempts to extract metadata.
     * 
     * <p>
     * Implementations should read the relevant sections of the image file and populate their
     * internal metadata structures.
     * </p>
     *
     * @return true if metadata was successfully extracted, otherwise false
     *
     * @throws ImageReadErrorException
     *         if the file format is invalid or the data cannot be interpreted
     *         as valid metadata
     * @throws IOException
     *         if a low-level I/O error occurs while reading the image file
     */
    boolean parseMetadata() throws ImageReadErrorException, IOException;
}