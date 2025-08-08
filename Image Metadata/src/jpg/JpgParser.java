package jpg;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import common.AbstractImageParser;
import common.BaseMetadata;
import common.DigitalSignature;
import common.ImageFileInputStream;
import common.ImageReadErrorException;
import common.Metadata;
import logger.LogFactory;
import tif.MetadataTIF;
import tif.TifParser;

/**
 * A parser for JPG image files that extracts metadata from the APP1 segment, specifically targeting
 * embedded EXIF data. This data is processed using an internal TIFF parser to provide a structured
 * metadata representation.
 *
 * <p>
 * Currently, this parser supports only the extraction of EXIF metadata. It expects well-formed APP1
 * segments beginning with the "Exif\0\0" identifier.
 * </p>
 *
 * <p>
 * <strong>Change History:</strong>
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
public class JpgParser extends AbstractImageParser
{
    private static final LogFactory LOGGER = LogFactory.getLogger(JpgParser.class);
    public static final byte[] JPG_EXIF_IDENTIFIER = "Exif\0\0".getBytes();

    /**
     * Constructs a new instance with the specified file path.
     *
     * @param fpath
     *        the path to the JPG file to be parsed
     * 
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public JpgParser(Path fpath) throws IOException
    {
        super(fpath);

        LOGGER.info("Image file [" + getImageFile() + "] loaded");

        String ext = getFileExtension();

        if (!ext.equalsIgnoreCase("jpg"))
        {
            LOGGER.warn("File [" + getImageFile().getFileName() + "] has an incorrect extension name. Found [" + ext + "], updating to [jpg]");
        }
    }

    /**
     * Constructs a new instance from a file path string.
     *
     * @param file
     *        the path to the JPG file as a string
     * 
     * @throws IOException
     *         if the file cannot be opened or read
     */
    public JpgParser(String file) throws IOException
    {
        this(Paths.get(file));
    }

    /**
     * Extracts raw segment data from the specified JPEG APP1 segment.
     *
     * @param stream
     *        the image stream to read from
     * @param segment
     *        the JPEG segment to search for (ie. APP1)
     * 
     * @return a byte array containing the EXIF payload, if found
     * @throws IOException
     *         if an error occurs while reading the specified stream
     */
    private byte[] readRawSegmentData(ImageFileInputStream stream, JpegSegmentConstants segment) throws IOException
    {
        byte segmentMarker;
        byte segmentFlagValue;
        int segmentLength = 0;
        JpegSegmentConstants EOS = JpegSegmentConstants.END_OF_IMAGE;

        do
        {
            /* Move to the beginning of the APP1 Segment position */
            segmentMarker = stream.readByte();
            segmentFlagValue = stream.readByte();

            if (segmentMarker == segment.marker && segmentFlagValue == segment.flag)
            {
                /* Take out 2 bytes for Data size descriptor itself */
                segmentLength = stream.readUnsignedShort() - 2;

                /* Verify there is an EXIF identifier to begin with */
                if (segmentLength > 0)
                {
                    byte[] exifHeader = stream.readBytes(JPG_EXIF_IDENTIFIER.length);

                    if (Arrays.equals(exifHeader, JPG_EXIF_IDENTIFIER))
                    {
                        /* Take out the length of the Exif identifier string */
                        segmentLength -= exifHeader.length;
                        byte[] rawSegmentBytes = stream.readBytes(segmentLength);

                        if (segmentLength != rawSegmentBytes.length)
                        {
                            throw new IllegalArgumentException("JPEG data segment in file [" + getImageFile() + "] does not meet the expected length of [" + segmentLength + "]");
                        }

                        return rawSegmentBytes;
                    }

                    else
                    {
                        throw new IllegalStateException("Unable to find an EXIF identifier within the [" + segment.description + "] segment");
                    }
                }

                else
                {
                    throw new IllegalArgumentException("Unable to find the segment [" + segment.description + "] in file [" + getImageFile() + "]");
                }
            }

        } while (segmentMarker != EOS.marker || segmentFlagValue != EOS.flag);

        throw new EOFException("Metadata is missing in file [" + getImageFile() + "]");
    }

    /**
     * Reads the metadata from a JPG file, if present, using the APP1 EXIF segment.
     *
     * @return a populated {@link Metadata} object containing the metadata
     * 
     * @throws ImageReadErrorException
     *         if the file is unreadable
     */
    @Override
    public Metadata<? extends BaseMetadata> readMetadata() throws ImageReadErrorException
    {
        byte[] app1SegmentBytes = null;

        try (InputStream fis = Files.newInputStream(getImageFile()))
        {
            ImageFileInputStream ImageStream = new ImageFileInputStream(fis);

            app1SegmentBytes = readRawSegmentData(ImageStream, JpegSegmentConstants.APP1_SEGMENT);
            metadata = TifParser.parseFromSegmentBytes(app1SegmentBytes);
        }

        catch (EOFException exc)
        {
            if (app1SegmentBytes == null)
            {
                LOGGER.warn("Metadata information not found in file [" + getImageFile() + "]");
            }
        }

        catch (NoSuchFileException exc)
        {
            throw new ImageReadErrorException("File [" + getImageFile() + "] does not exist", exc);
        }

        catch (IOException exc)
        {
            throw new ImageReadErrorException(exc);
        }

        return getSafeMetadata();
    }

    /**
     * Returns the previously parsed metadata from the JPG file.
     *
     * @return the metadata object, or an empty one if none was found
     */
    @Override
    public Metadata<? extends BaseMetadata> getSafeMetadata()
    {
        if (metadata == null)
        {
            LOGGER.warn("Metadata information has not been parsed yet.");
            return new MetadataTIF();
        }

        return metadata;
    }

    /**
     * Returns the detected {@code JPG} format.
     *
     * @return a {@link DigitalSignature} enum constant representing this image format
     */
    @Override
    public DigitalSignature getImageFormat()
    {
        return DigitalSignature.JPG;
    }
}