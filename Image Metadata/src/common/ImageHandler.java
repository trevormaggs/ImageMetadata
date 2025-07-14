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
 * @author Trevor Maggs, <a href="mailto:trevmaggs@tpg.com.au">trevmaggs@tpg.com.au</a>
 * @since 21 June 2025
 */
public interface ImageHandler
{
    /**
     * Processes the image data stream and returns a metadata representation of the image file. This
     * may include directories, fields, and values extracted from the file's internal structure.
     *
     * @return a {@link Metadata} instance containing structured metadata units
     * @throws ImageReadErrorException
     *         if the image is malformed or cannot be interpreted
     * @throws IOException
     *         if an I/O error occurs during image processing
     */
    Metadata<? extends BaseMetadata> processMetadata() throws ImageReadErrorException, IOException;
}
