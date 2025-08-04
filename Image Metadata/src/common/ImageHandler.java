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
 * <p>
 * Change History:
 * </p>
 *
 * <ul>
 * <li>Version 1.0 - Initial release by Trevor Maggs on 21 June 2025</li>
 * </ul>
 *
 * @version 0.1
 * @author Trevor Maggs
 * @since 21 June 2025
 */
public interface ImageHandler
{
    /**
     * Parses the image data stream and attempts to extract metadata.
     *
     * <p>
     * This method initiates the metadata extraction process for the image. Implementations should
     * read the appropriate sections of the image file and collect any available metadata into their
     * internal structures.
     * </p>
     *
     * @return true if metadata was successfully extracted, otherwise false
     *
     * @throws ImageReadErrorException
     *         if an error occurs while parsing the file
     * @throws IOException
     *         if there is a problem reading the image file
     */
    public boolean parseMetadata() throws ImageReadErrorException, IOException;
}