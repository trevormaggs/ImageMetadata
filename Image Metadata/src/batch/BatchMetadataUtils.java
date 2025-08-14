package batch;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.png.AbstractPngText;
import org.apache.commons.imaging.formats.png.PngImageParser;
import org.apache.commons.imaging.formats.png.PngImagingParameters;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;

public final class BatchMetadataUtils
{
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
        BasicFileAttributeView target = Files.getFileAttributeView(fpath, BasicFileAttributeView.class);
        target.setTimes(fileTime, fileTime, fileTime);
    }

    /**
     * Copies a JPEG image file to a target location and updates its EXIF {@code Date Taken}
     * metadata.
     *
     * <p>
     * This method updates the following EXIF fields in a lossless manner (without re-compressing
     * the image data):
     *
     * <ul>
     * <li>{@link org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants#EXIF_TAG_DATE_TIME_ORIGINAL}</li>
     * <li>{@link org.apache.commons.imaging.formats.tiff.constants.ExifTagConstants#EXIF_TAG_DATE_TIME_DIGITIZED}</li>
     * </ul>
     * </p>
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
    public static void updateDateTakenMetadataJPG(File sourceFile, File targetFile, FileTime datetime) throws IOException
    {
        try (FileOutputStream fos = new FileOutputStream(targetFile); BufferedOutputStream os = new BufferedOutputStream(fos))
        {
            TiffOutputSet outputSet = null;
            ImageMetadata metadata = Imaging.getMetadata(sourceFile);
            JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
            String dateTaken = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(datetime.toMillis());

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
     * @throws IOException
     *         if an error occurs during reading, writing, or processing the image
     */
    public static void updateDateTakenTextualPNG(File sourceFile, File targetFile, FileTime datetime) throws IOException
    {
        BufferedImage image = Imaging.getBufferedImage(sourceFile);
        PngImagingParameters writeParams = new PngImagingParameters();

        List<AbstractPngText> writeTexts = new ArrayList<>();
        writeTexts.add(new AbstractPngText.Text("Creation Time", new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(datetime.toMillis())));
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
}