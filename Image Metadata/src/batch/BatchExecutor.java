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
public class BatchExecutor implements Iterable<MetaMedia>
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
    private final String[] fileSet;
    private long dateOffsetUpdate;

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
    protected BatchExecutor(BatchBuilder builder) throws BatchErrorException
    {
        this.sourceDir = Paths.get(builder.bd_sourceDir);
        this.prefix = builder.bd_prefix;
        this.targetDir = Paths.get(builder.bd_target);
        this.embedDateTime = builder.bd_embedDateTime;
        this.skipMediaFiles = builder.bd_skipMediaFiles;
        this.debug = builder.bd_debug;
        this.datetime = builder.bd_datetime;
        this.fileSet = Arrays.copyOf(builder.bd_files, builder.bd_files.length);
        this.dateOffsetUpdate = 0L;

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

            if (fileSet != null && fileSet.length > 0)
            {
                for (String fileName : fileSet)
                {
                    Path fpath = sourceDir.resolve(fileName);

                    if (Files.exists(fpath) && Files.isRegularFile(fpath))
                    {
                        visitor.visitFile(fpath, Files.readAttributes(fpath, BasicFileAttributes.class));
                    }

                    else
                    {
                        LOGGER.warn("Skipping non-regular file [" + fpath + "]");
                        continue;
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

        for (MetaMedia file : imageSet)
        {
            System.out.printf("%s\n", file);
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
     * Indicates whether the user-provided date-time should be prepended to the
     * resulting copied file name.
     *
     * @return true if the date-time should be prepended, otherwise false
     */
    protected boolean embedDateTime()
    {
        return embedDateTime;
    }

    /**
     * Returns whether video files in the source directory should be skipped.
     *
     * @return true if video files should be ignored, otherwise false
     */
    protected boolean skipVideoFiles()
    {
        return skipMediaFiles;
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
                if (!dir.equals(sourceDir))
                {
                    LOGGER.info("Sub-directory [" + dir + "] is being read");
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path fpath, BasicFileAttributes attr) throws IOException
            {
                try
                {
                    MetaMedia media = processFile(fpath, attr, datetime, dateOffsetUpdate++);

                    if (media != null)
                    {
                        imageSet.add(media);
                    }
                }

                catch (ImageReadErrorException exc)
                {
                    String msg = "Failed to read image metadata from [" + fpath + "]";
                    LOGGER.error(msg, exc);

                    throw new IOException(msg, exc);
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
     * Processes the specified file and returns a {@link MetaMedia} object with the determined
     * {@code Date Taken} time-stamp.
     *
     * <p>
     * Metadata sources are checked in the following order:
     * </p>
     *
     * <ol>
     * <li>User-provided date</li>
     * <li>Image metadata (EXIF, PNG textual, etc.)</li>
     * <li>File's last modified time</li>
     * </ol>
     *
     * @param fpath
     *        the path representing the file to be processed
     * @param attr
     *        the attributes of the file
     * @param userDateTime
     *        a user-defined date that overrides all other date sources
     * @param dateOffset
     *        an offset value to ensure unique time-stamps for user-provided dates
     *
     * @return a populated {@link MetaMedia} object, or null if unsupported
     *
     * @throws IOException
     *         if an error occurs while reading the file
     * @throws ImageReadErrorException
     *         in the event of image parsing problems
     */
    private static MetaMedia processFile(Path fpath, BasicFileAttributes attr, String userDateTime, long dateOffset) throws IOException, ImageReadErrorException
    {
        Date metadataDate = null;
        boolean emptyMetadata = false;
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

                    if (dir != null)
                    {
                        metadataDate = dir.getDate(EXIF_TAG_DATE_TIME_ORIGINAL);
                    }
                }
            }

            else if (meta instanceof MetadataPNG)
            {
                MetadataPNG<?> png = (MetadataPNG<?>) meta;

                if (png.hasExifData())
                {
                    MetadataTIF tif = (MetadataTIF) png.getDirectory(MetadataTIF.class);
                    DirectoryIFD dir = tif.getDirectory(DirectoryIdentifier.EXIF_DIRECTORY_SUBIFD);

                    if (dir != null)
                    {
                        metadataDate = dir.getDate(EXIF_TAG_DATE_TIME_ORIGINAL);
                    }
                }

                else if (png.hasTextualData())
                {
                    ChunkDirectory dir = (ChunkDirectory) png.getDirectory(ChunkType.Category.TEXTUAL);

                    if (dir != null)
                    {
                        List<TextEntry> data = dir.getTextualData(TextKeyword.CREATE);

                        if (!data.isEmpty())
                        {
                            metadataDate = DateParser.convertToDate(data.get(0).getValue());
                        }
                    }
                }
            }

            else
            {
                LOGGER.info("File [" + fpath + "] is an unknown or unsupported image file");
            }
        }

        else
        {
            emptyMetadata = true;
            LOGGER.info("Metadata cannot be found [" + scanner.getFile() + "]");
        }

        FileTime modifiedTime = resolveDateTaken(metadataDate, fpath, attr, userDateTime, dateOffset);

        return new MetaMedia(fpath, modifiedTime, format, emptyMetadata);
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
     *        the date retrieved from the image's metadata, for example: EXIF, PNG text
     * @param fpath
     *        the path to the image file
     * @param attr
     *        the file's basic attributes, used as a fallback value to get the last modified time
     * @param userDateTaken
     *        a user-defined date that overrides all other date sources
     * @param dateOffset
     *        an offset value to ensure unique time-stamps for user-provided dates
     *
     * @return a {@link FileTime} object representing the determined "Date Taken" time-stamp
     */
    private static FileTime resolveDateTaken(Date metadataDate, Path fpath, BasicFileAttributes attr, String userDateTime, long dateOffset)
    {
        /* 1. User-provided date takes highest precedence */
        if (userDateTime != null && !userDateTime.isEmpty())
        {
            Date userDate = DateParser.convertToDate(userDateTime);

            if (userDate != null)
            {
                long newTime = userDate.getTime() + (dateOffset * DATE_OFFSET_MILLIS);

                LOGGER.info("Date Taken for [" + fpath.getFileName() + "] updated with user-defined date [" + userDate + "] plus offset [" + dateOffset + "]");
                return FileTime.fromMillis(newTime);
            }
        }

        /* 2. Fallback to metadata date */
        if (metadataDate != null)
        {
            LOGGER.info("Date Taken for [" + fpath.getFileName() + "] using metadata date [" + metadataDate + "]");
            return FileTime.fromMillis(metadataDate.getTime());
        }

        /* 3. Final fallback to file's last modified time */
        LOGGER.info("No metadata date found for [" + fpath.getFileName() + "]. Using file's last modified date [" + attr.lastModifiedTime() + "]");

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
}