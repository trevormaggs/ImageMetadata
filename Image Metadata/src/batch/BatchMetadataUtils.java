package batch;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.png.AbstractPngText;
import org.apache.commons.imaging.formats.png.PngImageParser;
import org.apache.commons.imaging.formats.png.PngImagingParameters;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.imaging.formats.tiff.TiffImagingParameters;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.imaging.formats.webp.WebPImageMetadata;

public final class BatchMetadataUtils
{
    private static final SimpleDateFormat DATETAKEN = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    // prevent instantiation
    private BatchMetadataUtils()
    {
    }

    /**
     * Returns the extension of the image file name, excluding the dot.
     *
     * <p>
     * If the file name does not contain an extension, an empty string is returned.
     * </p>
     *
     * @param fpath
     *        the file path
     *
     * @return the file extension, for example: {@code "jpg"} or {@code "png"} etc, or an empty
     *         string if none
     */
    public static String getFileExtension(Path fpath)
    {
        String fileName = fpath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');

        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1).toLowerCase();
    }

    /**
     * Sets the last modified time, last accessed time, and creation time of an image file.
     *
     * @param fpath
     *        the file path to modify
     * @param fileTime
     *        the file time to set
     *
     * @throws IOException
     *         if an error occurs while setting the file times
     */
    public static void changeFileTimeProperties(Path fpath, FileTime fileTime) throws IOException
    {
        BasicFileAttributeView attr = Files.getFileAttributeView(fpath, BasicFileAttributeView.class);

        attr.setTimes(fileTime, fileTime, fileTime);
    }

    /**
     * Retrieves the BasicFileAttributeView for the specified file path. This view provides access
     * to basic file attributes such as creation time, modification time, and file size.
     *
     * @param fpath
     *        the Path object representing the file. Must not be null and the file must exist
     * 
     * @return a BasicFileAttributeView instance for the specified file
     * 
     * @throws FileNotFoundException
     *         if the provided path is null or the file does not exist
     */
    public static BasicFileAttributeView getFileAttributeView(Path fpath) throws FileNotFoundException
    {
        if (fpath == null || Files.notExists(fpath))
        {
            throw new FileNotFoundException("File [" + fpath + "] path does not exist or is invalid");
        }

        return Files.getFileAttributeView(fpath, BasicFileAttributeView.class);
    }

    /**
     * Reads a source TIFF file, updates a specific metadata tag, and writes the
     * result to a new destination TIFF file.
     *
     * @param sourceFile
     *        the original TIFF file
     * @param targetFile
     *        the new file to save the updated TIFF
     * @param datetime
     *        the desired captured date-time to embed in the EXIF metadata
     * 
     * @throws IOException
     *         If an I/O error occurs
     */
    public static void updateDateTakenMetadataTIF(File sourceFile, File targetFile, FileTime datetime) throws IOException
    {
        TiffOutputSet outputSet = null;
        String dt = DATETAKEN.format(datetime.toMillis());
        ImageMetadata meta = Imaging.getMetadata(sourceFile);
        BufferedImage image = Imaging.getBufferedImage(sourceFile);

        if (meta instanceof TiffImageMetadata)
        {
            outputSet = ((TiffImageMetadata) meta).getOutputSet();
        }

        if (outputSet == null)
        {
            outputSet = new TiffOutputSet();
        }

        TiffOutputDirectory exif = outputSet.getOrCreateExifDirectory();

        exif.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        exif.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, dt);
        exif.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
        exif.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, dt);

        TiffImagingParameters params = new TiffImagingParameters();
        params.setOutputSet(outputSet);

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(targetFile)))
        {
            new TiffImageParser().writeImage(image, os, params);
        }
    }

    /**
     * Copies a JPEG image file to a target location and updates its EXIF {@code Date Taken}
     * metadata.
     *
     * <p>
     * This method updates the following EXIF fields in a lossless manner (without re-compressing
     * the image data):
     * </p>
     *
     * <ul>
     * <li>{@link org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants#EXIF_TAG_DATE_TIME_ORIGINAL}</li>
     * <li>{@link org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants#EXIF_TAG_DATE_TIME_DIGITIZED}</li>
     * </ul>
     *
     * <p>
     * The {@code datetime} parameter is converted internally to the EXIF date-time format
     * {@code yyyy:MM:dd HH:mm:ss}, using the milliseconds since epoch provided by
     * {@link java.nio.file.attribute.FileTime}.
     * </p>
     *
     * <p>
     * <b>Note:</b> this is a temporary solution using Apache Commons Imaging libraries. There are
     * plans to implement local libraries for direct metadata writing without external dependencies.
     * </p>
     *
     * @param sourceFile
     *        the original JPEG file to be copied
     * @param targetFile
     *        the destination file where the copy will be written
     * @param datetime
     *        the desired captured date-time to embed in the EXIF metadata
     *
     * @throws FileNotFoundException
     *         if the target file cannot be created or its parent directory does not exist
     * @throws IOException
     *         if an I/O error occurs during reading or writing the image
     */
    public static void updateDateTakenMetadataJPG(File sourceFile, File targetFile, FileTime datetime) throws FileNotFoundException, IOException
    {
        try (FileOutputStream fos = new FileOutputStream(targetFile); BufferedOutputStream os = new BufferedOutputStream(fos))
        {
            TiffOutputSet outputSet = null;
            ImageMetadata metadata = Imaging.getMetadata(sourceFile);
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            String dateTaken = DATETAKEN.format(datetime.toMillis());

            if (jpegMetadata != null)
            {
                TiffImageMetadata exif = jpegMetadata.getExif();

                if (exif != null)
                {
                    outputSet = exif.getOutputSet();
                }
            }

            if (outputSet == null)
            {
                outputSet = new TiffOutputSet();
            }

            TiffOutputDirectory exifDirectory = outputSet.getOrCreateExifDirectory();

            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, dateTaken);

            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, dateTaken);

            new ExifRewriter().updateExifMetadataLossless(sourceFile, os, outputSet);
        }
    }

    /**
     * Copies a PNG image file to a target location and updates its {@code Date Taken} property
     * within the PNG textual chunk.
     *
     * <p>
     * The method updates a textual chunk (named "Creation Time") using the specified date-time
     * parameter, in the format {@code yyyy:MM:dd HH:mm:ss}. The image is written in a lossless
     * manner without altering pixel data.
     * </p>
     *
     * <p>
     * <b>Note:</b> This is a temporary solution using Apache Commons Imaging libraries. Future
     * plans include implementing local code for direct PNG metadata writing (updating textual
     * chunks) without external dependencies.
     * </p>
     *
     * @param sourceFile
     *        the original PNG file to be copied
     * @param targetFile
     *        the destination file where the copy will be written
     * @param datetime
     *        the desired captured date-time to embed in the PNG textual chunk
     * 
     * @throws ImagingException
     *         in the event of a processing error while reading an image
     * @throws IOException
     *         if an error occurs during reading, writing, or processing the image
     */
    public static void updateDateTakenTextualPNG(File sourceFile, File targetFile, FileTime datetime) throws ImagingException, IOException
    {
        BufferedImage image = Imaging.getBufferedImage(sourceFile);
        PngImagingParameters writeParams = new PngImagingParameters();
        List<AbstractPngText> writeTexts = new ArrayList<>();

        writeTexts.add(new AbstractPngText.Text("Creation Time", DATETAKEN.format(datetime.toMillis())));
        writeParams.setTextChunks(writeTexts);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            PngImageParser writer = new PngImageParser();
            writer.writeImage(image, baos, writeParams);

            try (FileOutputStream fos = new FileOutputStream(targetFile); BufferedOutputStream os = new BufferedOutputStream(fos))
            {
                os.write(baos.toByteArray());
            }
        }
    }

    // TESTING

    /**
     * Converts the input date string to a Date object by attempting to parse it against a
     * predefined set of common date and time formats.
     *
     * @param input
     *        the input date string
     * 
     * @return the converted Date object
     * 
     * @throws NullPointerException
     *         if the input is null
     * @throws IllegalArgumentException
     *         if the date format is invalid or not supported
     */
    public static Date convertToDate2(String input)
    {
        if (input == null)
        {
            throw new NullPointerException("Date input is null");
        }

        // Define a comprehensive list of date-time formats to try
        // Prioritise common formats first for efficiency
        List<String> validFormats = Arrays.asList(
                "yyyy:MM:dd HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy/MM/dd HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "MM/dd/yyyy HH:mm:ss",
                "MM-dd-yyyy HH:mm:ss",
                "yyyy:MM:dd",
                "yyyy-MM-dd",
                "yyyy/MM/dd",
                "dd/MM/yyyy",
                "dd-MM-yyyy");

        for (String format : validFormats)
        {
            try
            {
                // Use a non-lenient parser for strict format matching
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
                simpleDateFormat.setLenient(false);

                return simpleDateFormat.parse(input.trim());
            }

            catch (ParseException exc)
            {
                // Ignore and try the next format
            }
        }

        // If no format matched, throw an exception
        throw new IllegalArgumentException("Date [" + input + "] is in an invalid or unsupported format");
    }

    /**
     * Reads a source WebP file, updates a specific Exif metadata tag, and writes the
     * result to a new destination WebP file.
     *
     * @param sourceFile
     *        The original WebP file.
     * @param destFile
     *        The new file to save the updated WebP.
     * @param datetime
     *        the desired captured date-time to embed in the EXIF metadata
     * @throws IOException
     *         If an I/O error occurs.
     * @throws ImagingException
     *         If the file cannot be read or written.
     */
    public static void updateWebpMetadata(File sourceFile, File destFile, FileTime datetime) throws ImagingException, IOException
    {
        File webpImageFile = sourceFile;
        ImageMetadata metadata2 = Imaging.getMetadata(webpImageFile);
        WebPImageMetadata webpMetadata = (WebPImageMetadata) metadata2;

        TiffImageMetadata exif = null;

        if (webpMetadata != null && webpMetadata.getExif() != null)
        {
            exif = webpMetadata.getExif();
        }

        TiffOutputSet outputSet;

        if (exif != null)
        {
            outputSet = exif.getOutputSet();
        }

        else
        {
            outputSet = new TiffOutputSet(); // Create a new TiffOutputSet if no existing EXIF
        }

        String dateTaken = DATETAKEN.format(datetime.toMillis());

        // Get or create the root directory (IFD0)
        TiffOutputDirectory exifDirectory = outputSet.getOrCreateRootDirectory();

        // Modify existing tags or add new ones
        // Example: Update the Artist tag
        exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
        exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, dateTaken);

        File outputFile = new File("output.webp");

        try (OutputStream os = new FileOutputStream(outputFile))
        {
            new ExifRewriter().updateExifMetadataLossless(webpImageFile, os, outputSet);
        }
    }
}