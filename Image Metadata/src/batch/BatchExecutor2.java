package batch;

import static tif.TagEntries.TagEXIF.EXIF_TAG_DATE_TIME_ORIGINAL;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import common.DateParser;
import common.DigitalSignature;
import common.ImageReadErrorException;
import common.Metadata;
import common.SystemInfo;
import logger.LogFactory;
import png.ChunkDirectory;
import png.ChunkType;
import png.MetadataPNG;
import png.TextEntry;
import png.TextKeyword;
import tif.DirectoryIFD;
import tif.DirectoryIdentifier;
import tif.MetadataTIF;

/**
 * <p>
 * Automates the batch processing of image files by copying, renaming, and chronologically sorting
 * them based on their EXIF metadata, such as {@code DateTimeOriginal}.
 * </p>
 *
 * <p>
 * Supported formats include JPEG, TIFF, PNG, WebP and HEIF. If EXIF metadata is unavailable, the
 * file's last modified time-stamp is assumed.
 * </p>
 *
 * <p>
 * To access the sorted set of files, iterate using the {@code Iterable<MetaMedia>} interface.
 * </p>
 *
 * @see MetaMedia
 * @version 1.0
 * @author Trevor Maggs
 * @since 4 August 2025
 */
public class BatchExecutor2 implements Iterable<MetaMedia>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(BatchExecutor.class);
    public static final String DEFAULT_SOURCE_DIRECTORY = ".";
    public static final String DEFAULT_TARGET_DIRECTORY = "IMAGEDIR";
    public static final String DEFAULT_IMAGE_PREFIX = "image";
    private static final long DATE_OFFSET_MILLIS = 10_000L;

    private final String prefix;
    private final Path sourceDir;
    private final Path targetDir;
    private final Set<MetaMedia> imageSet;
    private final boolean embedDateTime;
    private final boolean skipMediaFiles;
    private final boolean debug;
    private final String datetime;
    private final String[] fileGroup;
    private final AtomicLong userDateOffset;

    /**
     * Constructs a BatchExecutor using the specified {@link BatchBuilder} configuration. This
     * constructor is package-private and should be invoked via {@link BatchBuilder#build()}.
     *
     * @param builder
     *        Builder object containing required parameters
     *
     * @throws BatchErrorException
     *         in the event of an error during batch processing
     */
    private BatchExecutor2(BatchBuilder builder) throws BatchErrorException
    {
        this.sourceDir = Paths.get(builder.bd_sourceDir);
        this.prefix = builder.bd_prefix;
        this.targetDir = Paths.get(builder.bd_target);
        this.embedDateTime = builder.bd_embedDateTime;
        this.skipMediaFiles = builder.bd_skipMediaFiles;
        this.debug = builder.bd_debug;
        this.datetime = builder.bd_datetime;
        this.fileGroup = Arrays.copyOf(builder.bd_files, builder.bd_files.length);
        this.userDateOffset = new AtomicLong(0);

        if (!Files.isDirectory(sourceDir))
        {
            throw new BatchErrorException("The source directory [" + sourceDir + "] is not a valid directory. Please verify that the path exists and is a directory");
        }

        if (builder.bd_descending)
        {
            // Sorts the copied images in descending order
            imageSet = new TreeSet<>(new DescendingTimestampComparator());
            LOGGER.info("Sorted copied images in descending order.");
        }

        else
        {
            // Sorts the copied images in ascending order
            imageSet = new TreeSet<>(new DefaultTimestampComparator());
            LOGGER.info("Sorted copied images in ascending order.");
        }
    }

    /**
     * Starts the batch processing workflow: cleans the target directory, begins logging, and
     * processes the source directory or a set of files if supplied.
     *
     * @throws BatchErrorException
     *         if any I/O or metadata error occurs
     */
    protected void start() throws BatchErrorException
    {
        FileVisitor<Path> visitor = createImageVisitor();

        try
        {
            if (Files.exists(targetDir))
            {
                deleteTargetDirectory(targetDir);
            }

            Files.createDirectories(targetDir);

            startLogging();

            if (fileGroup != null && fileGroup.length > 0)
            {
                for (String fileName : fileGroup)
                {
                    Path fpath = sourceDir.resolve(fileName);

                    if (Files.exists(fpath) && Files.isRegularFile(fpath))
                    {
                        visitor.visitFile(fpath, Files.readAttributes(fpath, BasicFileAttributes.class));
                    }
                }
            }

            else
            {
                Files.walkFileTree(sourceDir, visitor);
            }
        }

        catch (IOException exc)
        {
            throw new BatchErrorException("An I/O error has occurred", exc);
        }
    }

    /**
     * Retrieves the source directory where all original files are found.
     *
     * @return the Path instance of the source directory
     */
    protected Path getSourceDirectory()
    {
        return sourceDir;
    }

    /**
     * Retrieves the target directory where all copied files are saved.
     *
     * @return the Path instance of the target directory
     */
    protected Path getTargetDirectory()
    {
        return targetDir;
    }

    /**
     * Returns the prefix used when renaming each copied image file.
     *
     * @return the filename prefix
     */
    protected String getPrefix()
    {
        return prefix;
    }

    /**
     * Returns the number of image files identified and sorted after batch processing.
     *
     * @return the total count of processed images
     */
    protected int getImageCount()
    {
        return imageSet.size();
    }

    /**
     * Returns an iterator over the internal sorted set of {@code MetaMedia} objects.
     *
     * @return an Iterator instance for navigating the MetaMedia set
     */
    @Override
    public Iterator<MetaMedia> iterator()
    {
        return imageSet.iterator();
    }

    /**
     * Creates and returns a {@link FileVisitor} instance to traverse the specified source
     * directory.
     * 
     * <p>
     * This visitor processes each file by extracting and analysing its metadata, such as the EXIF
     * {@code DateTimeOriginal} tag. If a file contains relevant metadata, it is wrapped in a
     * {@link MetaMedia} object and added to the internal sorted set.
     * </p>
     * 
     * @return a configured {@link FileVisitor} for processing image files
     */
    private FileVisitor<Path> createImageVisitor()
    {
        return new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
            {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path fpath, BasicFileAttributes attr) throws IOException
            {
                try
                {
                    MetaMedia media = processFile(fpath, attr, datetime, userDateOffset);

                    if (media != null)
                    {
                        imageSet.add(media);
                    }
                }

                catch (ImageReadErrorException exc)
                {
                    throw new IOException(exc);
                }

                return FileVisitResult.CONTINUE;
            }
        };
    }

    /**
     * Begins the logging system and writes initial configuration details to the log file. This
     * method is for internal setup and not intended for external use.
     *
     * @throws BatchErrorException
     *         if the logging service cannot be set up
     */
    private void startLogging() throws BatchErrorException
    {
        try
        {
            // Set up the file for logging and disable the console handler
            String logFilePath = getTargetDirectory() + "/batchlog_" + SystemInfo.getHostname() + ".log";

            LOGGER.configure(logFilePath);
            LOGGER.setDebug(debug);
            LOGGER.setTrace(false);

            // Log some information about the logging setup
            LOGGER.info("Log level set to [" + LOGGER.getVerbosityLevel() + "] by default for logging");
            LOGGER.info("Source directory set to [" + getSourceDirectory().toAbsolutePath() + "] with original images");
            LOGGER.info("Target directory set to [" + getTargetDirectory().toAbsolutePath() + "] for copied images");
        }

        catch (SecurityException | IOException exc)
        {
            throw new BatchErrorException("Unable to start logging. Program terminated", exc);
        }
    }

    /**
     * Processes the specified file and adds to the data structure according to the file signature
     * for efficient retrieval.
     *
     * @param fpath
     *        the path representing the file to be processed
     * @param attr
     *        the attributes of the file
     * @param userDateTime
     *        a user-defined date that overrides all other date sources
     * @param userDateOffset
     *        a synchronised offset to ensure unique time-stamps for user-provided dates
     * 
     * @throws IOException
     *         if an error occurs while reading the file
     * @throws ImageReadErrorException
     *         in the event of image parsing problems
     */
    private static MetaMedia processFile(Path fpath, BasicFileAttributes attr, String userDateTime, AtomicLong userDateOffset) throws IOException, ImageReadErrorException
    {
        MetaMedia media = null;
        FileTime modifiedTime = null;

        TestScanner scanner = TestScanner.loadImage(fpath);
        DigitalSignature format = scanner.getImageFormat();
        Metadata<?> meta = scanner.readMetadata();

        if (meta != null && meta.hasMetadata())
        {
            if (meta instanceof MetadataTIF)
            {
                MetadataTIF tif = (MetadataTIF) meta;

                if (tif.hasExifData())
                {
                    DirectoryIFD dir = tif.getDirectory(DirectoryIdentifier.EXIF_DIRECTORY_SUBIFD);
                    modifiedTime = resolveDateTaken(dir.getDate(EXIF_TAG_DATE_TIME_ORIGINAL), fpath, attr, userDateTime, userDateOffset);
                }

                media = new MetaMedia(fpath, modifiedTime, format);
            }

            else if (meta instanceof MetadataPNG)
            {
                MetadataPNG<?> png = (MetadataPNG<?>) meta;

                // Check for EXIF data first
                if (png.hasExifData())
                {
                    MetadataTIF tif = (MetadataTIF) png.getDirectory(MetadataTIF.class);
                    DirectoryIFD dir = tif.getDirectory(DirectoryIdentifier.EXIF_DIRECTORY_SUBIFD);
                    modifiedTime = resolveDateTaken(dir.getDate(EXIF_TAG_DATE_TIME_ORIGINAL), fpath, attr, userDateTime, userDateOffset);
                }
                // Fallback to textual data if no EXIF is present
                else if (png.hasTextualData())
                {
                    ChunkDirectory dir = (ChunkDirectory) png.getDirectory(ChunkType.Category.TEXTUAL);
                    List<TextEntry> data = dir.getTextualData(TextKeyword.CREATE);

                    for (TextEntry element : data)
                    {
                        // Assuming only one CREATE date is relevant, break after the first one
                        modifiedTime = resolveDateTaken(DateParser.convertToDate(element.getValue()), fpath, attr, userDateTime, userDateOffset);

                        System.out.printf("Textual in %s - %s\n\n", fpath.getFileName(), element.getValue());
                        break;
                    }
                }

                media = new MetaMedia(fpath, modifiedTime, format);
            }

            else
            {
                LOGGER.info(String.format("File [%s] is an unknown or unsupported image file", fpath));
            }
        }

        else
        {
            LOGGER.info(String.format("Metadata cannot be found [%s]", scanner.getFile()));
            modifiedTime = resolveDateTaken(null, fpath, attr, userDateTime, userDateOffset);
            media = new MetaMedia(fpath, modifiedTime, format);
        }

        return media;
    }

    /**
     * Determines the appropriate {@code Date Taken} time-stamp for the image file.
     *
     * <p>
     * The method prioritises date sources based on the following order:
     * </p>
     *
     * <ol>
     * <li>A user-provided date, if available</li>
     * <li>The date from image metadata, for example: EXIF or Textual chunk, if available</li>
     * <li>The file's last modified time-stamp</li>
     * </ol>
     *
     * <p>
     * If a user-provided date is used, a 10-second offset is applied to prevent duplicate
     * time-stamps for multiple files processed with the same date, ensuring proper sorting.
     * </p>
     *
     * @param metadataDate
     *        the date retrieved from the image's metadata, for example: EXIF, PNG text)
     * @param fpath
     *        the path to the image file
     * @param attr
     *        the file's basic attributes, used as a fallback value to get the last modified time
     * @param userDateTaken
     *        a user-defined date that overrides all other date sources
     * @param userDateOffset
     *        a synchronised offset to ensure unique time-stamps for user-provided dates
     *
     * @return a {@link FileTime} object representing the determined "Date Taken" time-stamp
     */
    private static FileTime resolveDateTaken(Date metadataDate, Path fpath, BasicFileAttributes attr, String userDateTime, AtomicLong userDateOffset)
    {
        // 1. User-provided date takes highest precedence
        if (userDateTime != null && !userDateTime.isEmpty())
        {
            Date userDate = DateParser.convertToDate(userDateTime);

            if (userDate != null)
            {
                long offset = userDateOffset.getAndIncrement();
                long newTime = userDate.getTime() + (offset * DATE_OFFSET_MILLIS);
                LOGGER.info(String.format("Date Taken for [%s] updated with user-defined date [%s] + offset [%d]", fpath.getFileName(), userDate, offset));

                return FileTime.fromMillis(newTime);
            }
        }

        // 2. Fallback to metadata date
        if (metadataDate != null)
        {
            LOGGER.info(String.format("Date Taken for [%s] using metadata date [%s]", fpath.getFileName(), metadataDate));
            return FileTime.fromMillis(metadataDate.getTime());
        }

        // 3. Final fallback to file's last modified time
        LOGGER.info(String.format("No metadata date found for [%s]. Using file's last modified date [%s]", fpath.getFileName(), attr.lastModifiedTime()));
        return attr.lastModifiedTime();
    }

    /**
     * Permanently deletes the target directory and all of its contents. This operation is
     * destructive and cannot be undone.
     *
     * @param dir
     *        the path of the directory to delete
     *
     * @throws IOException
     *         if an error occurs while deleting files or directories
     */
    private static void deleteTargetDirectory(Path dir) throws IOException
    {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException
            {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
            {
                if (exc == null)
                {
                    Files.delete(dir);
                }

                else
                {
                    throw exc;
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void main(String[] args)
    {
        // Example usage
        BatchBuilder builder = new BatchBuilder()
                .source("E:\\git\\ImageMetadata\\Image Metadata\\img")
                .target(DEFAULT_TARGET_DIRECTORY)
                .name("misty")
                .descending(false)
                // .datetime("07 Oct 2011") // Example of a user-defined date
                // .embedDateTime(cli.existsOption("-e"))
                .fileSet(new String[]{"IMG_0820.HEIC", "pool19.JPG", "gemmapreg.tif"})
                .debug(false);

        try
        {
            BatchExecutor batch = builder.build();
            batch.start();
        }

        catch (BatchErrorException exc)
        {
            exc.printStackTrace();
        }
    }
}