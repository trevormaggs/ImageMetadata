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
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
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
import common.CommandLineParser;
import common.ConsoleBar;
import common.ProjectBuildInfo;
import common.cli.CommandLineReader;
import logger.LogFactory;

/**
 * <p>
 * This class creates a console interface utility designed to read arguments from the command-line.
 * These arguments are then processed to copy image media files to a target directory, sorting them
 * by their {@code Date Taken} metadata property.
 * </p>
 *
 * <p>
 * During the process, it updates each file's creation date, last modification time, and last access
 * time to align with the corresponding {@code Date Taken} property. The sorted list can be either
 * in an ascending (default) or descending chronological order.
 * </p>
 *
 * <p>
 * Change History:
 * </p>
 *
 * <ul>
 * <li>Version 1.0 - Initial release by Trevor Maggs on 16 July 2024</li>
 * </ul>
 *
 * @author Trevor Maggs
 * @since 16 July 2024
 */
public final class BatchConsole2 extends BatchExecutor
{
    private static final LogFactory LOGGER = LogFactory.getLogger(BatchConsole2.class);

    /**
     * Constructs a new instance of this class, employing a Builder design pattern to process the
     * specified parameters and update the copied image files. The actual Builder implementation
     * exists in the superclass, {@link BatchImageEngine}.
     *
     * @param builder
     *        the Builder object containing parameters for constructing this instance.
     *
     * @throws BatchErrorException
     *         in case of an error during batch processing
     * @throws IOException
     * @throws FileNotFoundException
     */
    private BatchConsole2(BatchBuilder builder) throws BatchErrorException, IOException
    {
        super(builder);
        copyToTarget();
    }

    /**
     * Copies files from the source directory to the target directory, renaming each file with the
     * designated prefix. It also modifies the time-stamp attributes of each file, specifically
     * creation time, last modified time, and last accessed time to align with the
     * {@code Date Taken} metadata property.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws
     */
    public void copyToTarget2() throws IOException
    {
        int k = 0;
        Path copied;
        FileTime captureTime;
        SimpleDateFormat df = new SimpleDateFormat("_ddMMMyyyy");

        System.out.printf("Number of files found for batch processing: %d%n%n", getImageCount());

        for (MetaMedia media : this)
        {
            ConsoleBar.updateProgressBar(++k, getImageCount());

            String originalFileName = media.getPath().getFileName().toString();
            String fileExtension = getFileExtension(originalFileName);
            String fname;

            if (media.isVideoFormat())
            {
                if (skipVideoFiles())
                {
                    LOGGER.info(String.format("File [%s] skipped", media.getPath()));
                    continue;
                }

                fname = originalFileName.toLowerCase();

                LOGGER.info("File [" + media.getPath() + "] is a media type. Copied only");
            }

            else
            {
                fname = String.format("%s%d%s.%s", getPrefix(), k, (embedDateTime() ? df.format(media.getTimestamp()) : ""), fileExtension);
            }

            copied = getTargetDirectory().resolve(fname);
            captureTime = FileTime.fromMillis(media.getTimestamp());

            // Handle file type specific metadata updates
            if ((media.isJPG() || media.isTIF()) && media.isMetadataEmpty())
            {
                changeDateTimeMetadata(media.getPath().toFile(), copied.toFile(), captureTime);
            }

            else if (media.isPNG() && media.isMetadataEmpty())
            {
                updateDateTakenPNG(media.getPath().toFile(), copied.toFile(), captureTime);
            }

            else
            {
                // General file copy for files with existing metadata or other types
                Files.copy(media.getPath(), copied, StandardCopyOption.COPY_ATTRIBUTES);
            }

            // Update file system time properties for all copied files
            changeFileTimeProperties(copied, captureTime);
        }

        System.out.println();

        if (getImageCount() > 0)
        {
            System.out.printf("Images copied to directory: %s%n", getTargetDirectory().toAbsolutePath());
        }

        else
        {
            System.out.printf("No images found.%n");
            System.out.printf("Directory [%s] is empty.%n", getTargetDirectory().toAbsolutePath());
        }

        System.out.printf("Program completed.%n");
        LOGGER.info(String.format("Total number of copied images: %d", getImageCount()));
    }

    private String getFileExtension(String fileName)
    {
        int lastDot = fileName.lastIndexOf('.');

        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1).toLowerCase();
    }

    /**
     * Processes the command line arguments.
     *
     * @param arguments
     *        an array of strings containing the command line arguments
     *
     * @return a CommandLineReader object
     */
    private static CommandLineReader scanArguments(String[] arguments)
    {
        CommandLineReader cli = new CommandLineReader(arguments);

        try
        {
            // Define command argument rules
            cli.addRule("-t", CommandLineParser.ARG_OPTIONAL);
            cli.addRule("-e", CommandLineParser.ARG_BLANK);
            cli.addRule("-s", CommandLineParser.ARG_BLANK);
            cli.addRule("-l", CommandLineParser.ARG_OPTIONAL);
            cli.addRule("--desc", CommandLineParser.ARG_BLANK);
            cli.addRule("-m", CommandLineParser.ARG_OPTIONAL);
            cli.addRule("-f", CommandLineParser.SEP_OPTIONAL);
            cli.addRule("-c", CommandLineParser.ARG_BLANK);

            cli.addRule("-v", CommandLineParser.ARG_BLANK);
            cli.addRule("--version", CommandLineParser.ARG_BLANK);

            cli.addRule("-d", CommandLineParser.ARG_BLANK);
            cli.addRule("--debug", CommandLineParser.ARG_BLANK);

            cli.addRule("-h", CommandLineParser.ARG_BLANK);
            cli.addRule("--help", CommandLineParser.ARG_BLANK);

            cli.setMaximumStandaloneArgumentCount(1);
            cli.parse();

            if (cli.existsOption("-h") || cli.existsOption("--help"))
            {
                showHelp();
                System.exit(0);
            }

            if (cli.existsOption("-v") || cli.existsOption("--version"))
            {
                System.out.printf("Build date: %s%n", ProjectBuildInfo.getInstance(BatchConsole2.class).getBuildDate());
                System.exit(0);
            }

            if (cli.existsOption("-d") || cli.existsOption("--debug"))
            {
                LOGGER.setDebug(true);
            }
        }

        catch (ParseException exc)
        {
            System.err.println(exc.getMessage());
            showUsage();
            System.exit(1);
        }

        return cli;
    }

    /**
     * Prints the command usage line, showing the correct flag options.
     */
    private static void showUsage()
    {
        System.out.format("Usage: %s [-l label] [-t target directory] [-e] [-s] [-c] [-m date taken] [-f <File 1> ... <File n>] [--desc] [-d|--debug] [-v|--version] [-h|--help] <Source Directory>%n",
                ProjectBuildInfo.getInstance(BatchConsole2.class).getShortFileName());
    }

    /**
     * Prints detailed usage help information, providing guidance on how to use this program.
     */
    private static void showHelp()
    {
        showUsage();

        System.out.println();
        System.out.println("\t-l\t\t Label each copied file name with the designated prefix");
        System.out.println("\t-t\t\t Target directory where copied files are saved");
        System.out.println("\t-e\t\t Embed date and time stamp in copied file names");
        System.out.println("\t-m\t\t Modify file's Date Taken metadata property if empty");
        System.out.println("\t-f\t\t List of specified file names to process");
        System.out.println("\t-s\t\t Skip media files");
        System.out.println("\t-c\t\t Convert non-JPG files into the JPG format");
        System.out.println("\t--desc\t\t Sort the images in descending order");

        System.out.println("\t-v\t\t Display last build date");
        System.out.println("\t-h\t\t Display this help message and exit");
        System.out.println("\t-d\t\t Enable debugging (Not implemented)");

        System.out.println("\t--debug\t\t Same as -d");
        System.out.println("\t--version\t Same as -v");
        System.out.println("\t--help\t\t Same as -h");

        System.out.printf("%nExample: %s -p tahiti -t newdir c:\\Photos\\holidays %n%n", ProjectBuildInfo.getInstance(BatchConsole2.class).getShortFileName());
        System.out.printf("The above command copies image files from the source directory, 'c:\\Photos\\holidays' to "
                + "the target 'newdir' directory. Renames all copied images by prefixing them with 'tahiti' followed by an incremental index.%n%n");
    }

    /**
     * Begins the execution process by reading arguments from the command line and processing them.
     *
     * @param arguments
     *        an array of strings containing the command line arguments
     *
     * @throws BatchErrorException
     *         in case of an error during batch processing
     * @throws IOException
     */
    private static void start(String[] arguments) throws BatchErrorException, IOException
    {
        CommandLineReader cli = scanArguments(arguments);

        BatchBuilder batch = new BatchBuilder()
                .source(cli.getStandaloneArgumentCount() > 0 ? cli.getFirstStandaloneArgument() : DEFAULT_SOURCE_DIRECTORY)
                .target(cli.existsOption("-t") ? cli.getValueByOption("-t") : DEFAULT_TARGET_DIRECTORY)
                .name(cli.existsOption("-l") ? cli.getValueByOption("-l") : DEFAULT_IMAGE_PREFIX)
                .descending(cli.existsOption("--desc"))
                .datetime(cli.getValueByOption("-m"))
                .embedDateTime(cli.existsOption("-e"))
                .skipVideo(cli.existsOption("-s"));

        if (cli.existsOption("-f"))
        {
            String[] files = new String[cli.getValueLength("-f")];

            for (int k = 0; k < cli.getValueLength("-f"); k++)
            {
                files[k] = cli.getValueByOption("-f", k);
            }

            batch.fileSet(files);
        }

        batch.build();
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
    protected static void changeFileTimeProperties(Path fpath, FileTime fileTime) throws IOException
    {
        BasicFileAttributeView target = Files.getFileAttributeView(fpath, BasicFileAttributeView.class);
        target.setTimes(fileTime, fileTime, fileTime);
    }

    /**
     * Copies the specified original image file to the target file, and updates the
     * {@code Date Taken} property in the image metadata with the provided date-time parameter.
     *
     * @param sourceFile
     *        the original image file to be copied
     * @param targetFile
     *        the new target file, a copy of the specified source file
     * @param datetime
     *        The captured date-time to be updated
     *
     * @throws FileNotFoundException
     *         if the target file cannot be found
     * @throws IOException
     *         if an error occurs during processing
     */
    protected static void changeDateTimeMetadata(File sourceFile, File targetFile, FileTime datetime) throws FileNotFoundException, IOException
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

            // Recreate the 'Date Taken' field
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL, dateTaken);

            // Recreate the 'Digitized Date' field
            exifDirectory.removeField(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
            exifDirectory.add(ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED, dateTaken);

            new ExifRewriter().updateExifMetadataLossless(sourceFile, os, outputSet);
        }
    }

    /**
     * Copies the source image file to the target file and modifies the {@code Date Taken} property
     * within the PNG textual chunk using the specified date-time parameter.
     *
     * @param sourceFile
     *        the original image file to be copied
     * @param targetFile
     *        the destination file, a copy of the specified source file
     * @param datetime
     *        the Date Taken property time to be updated
     *
     * @throws IOException
     *         if an error occurs during processing
     */
    public static void updateDateTakenPNG(File sourceFile, File targetFile, FileTime datetime) throws IOException
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

            try (FileOutputStream fos = new FileOutputStream(targetFile); OutputStream os = new BufferedOutputStream(fos))
            {
                os.write(baos.toByteArray());
            }
        }
    }
}