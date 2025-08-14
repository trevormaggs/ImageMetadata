package batch;

import static tif.TagEntries.TagEXIF.EXIF_TAG_DATE_TIME_ORIGINAL;
import java.io.FileNotFoundException;
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
import common.AbstractImageParser;
import common.DateParser;
import common.DigitalSignature;
import common.ImageParserFactory;
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
 * Automates the batch processing of image files by copying, renaming, and chronologically sorting
 * them based on their EXIF metadata, such as {@code DateTimeOriginal}.
 *
 * <p>
 * This class supports a range of image formats, including JPEG, TIFF, PNG, WebP, and HEIF. If EXIF
 * metadata is not available, it defaults to using the file's last modified time-stamp.
 * </p>
 *
 * <p>
 * To access the sorted list of files, use the {@code Iterable<MetaMedia>} interface.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class BatchExecutor implements Batchable, Iterable<MetaMedia>
{
    private static final LogFactory LOGGER = LogFactory.getLogger(BatchExecutor.class);
    private static final long DATE_OFFSET_MILLIS = 10_000L;
    private static final FileVisitor<Path> DELETE_VISITOR;
    public static final String DEFAULT_SOURCE_DIRECTORY = ".";
    public static final String DEFAULT_TARGET_DIRECTORY = "IMAGEDIR";
    public static final String DEFAULT_IMAGE_PREFIX = "image";
    private final String prefix;
    private final Path sourceDir;
    private final Path targetDir;
    private final Set<MetaMedia> imageSet;
    private final boolean embedDateTime;
    private final boolean skipVideoFiles;
    private final boolean debug;
    private final String datetime;
    private final String[] fileSet;
    private long dateOffsetUpdate;

    static
    {
        DELETE_VISITOR = new SimpleFileVisitor<Path>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
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
        };
    }

    /**
     * Constructs a {@code BatchExecutor} using the provided {@link BatchBuilder} configuration.
     * This constructor is package-private and should be invoked through
     * {@link BatchBuilder#build()}.
     *
     * @param builder
     *        the builder object containing required parameters
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
        this.skipVideoFiles = builder.bd_skipVideoFiles;
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
     * Begins the batch processing workflow by cleaning the target directory, setting up logging,
     * and processing the specified source files or directory.
     *
     * @throws BatchErrorException
     *         if an I/O or metadata-related error occurs
     */
    protected void scan() throws BatchErrorException
    {
        FileVisitor<Path> visitor = createImageVisitor();

        try
        {
            if (Files.exists(targetDir))
            {
                /*
                 * Permanently deletes the target directory and all of its contents.
                 * This operation is destructive and cannot be undone.
                 */
                Files.walkFileTree(targetDir, DELETE_VISITOR);
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
    }

    /**
     * Retrieves the source directory where the original files are located.
     *
     * @return the {@code Path} instance of the source directory
     */
    protected Path getSourceDirectory()
    {
        return sourceDir;
    }

    /**
     * Retrieves the target directory where the processed files will be saved.
     *
     * @return the {@code Path} instance of the target directory
     */
    protected Path getTargetDirectory()
    {
        return targetDir;
    }

    /**
     * Returns the prefix used for renaming each copied image file.
     *
     * @return the filename prefix
     */
    protected String getPrefix()
    {
        return prefix;
    }

    /**
     * Returns the total number of image files identified and processed after a batch run.
     *
     * @return the count of processed images
     */
    protected int getImageCount()
    {
        return imageSet.size();
    }

    /**
     * Indicates whether the user-provided date-time should be prepended to the renamed file.
     *
     * @return {@code true} if the date-time should be prepended, otherwise {@code false}
     */
    protected boolean embedDateTime()
    {
        return embedDateTime;
    }

    /**
     * Returns whether video files in the source directory should be skipped during processing.
     *
     * @return {@code true} if video files should be ignored, otherwise {@code false}
     */
    protected boolean skipVideoFiles()
    {
        return skipVideoFiles;
    }

    /**
     * Returns an iterator over the internal sorted set of {@code MetaMedia} objects.
     *
     * @return an {@code Iterator} for navigating the {@code MetaMedia} set
     */
    @Override
    public Iterator<MetaMedia> iterator()
    {
        return imageSet.iterator();
    }

    /**
     * Creates and returns a {@link FileVisitor} instance to traverse the source directory.
     *
     * <p>
     * This visitor extracts and analyses metadata from each file, such as the EXIF
     * {@code DateTimeOriginal} tag. Files with relevant metadata are wrapped in a {@link MetaMedia}
     * object and added to the internal sorted set.
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
                    MetaMedia media = processFile(fpath, attr, datetime, dateOffsetUpdate);
                    dateOffsetUpdate++;

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
     * Begins the logging system and writes initial configuration details to a log file. This method
     * is for internal setup and is not intended for external use.
     *
     * @throws BatchErrorException
     *         if the logging service cannot be set up
     */
    private void startLogging() throws BatchErrorException
    {
        try
        {
            String logFilePath = Paths.get(targetDir.toString(), "batchlog_" + SystemInfo.getHostname() + ".log").toString();

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
     * Processes the specified file and returns a {@link MetaMedia} object containing the determined
     * {@code Date Taken} time-stamp.
     *
     * <p>
     * The method prioritises metadata sources in the following order:
     * </p>
     *
     * <ol>
     * <li>User-provided date</li>
     * <li>Image metadata, for example: EXIF, PNG textual data</li>
     * <li>File's last modified time</li>
     * </ol>
     *
     * @param fpath
     *        the path of the file to be processed
     * @param attr
     *        the basic attributes of the file
     * @param userDateTime
     *        a user-defined date string that overrides all other date sources
     * @param dateOffset
     *        a numeric offset to ensure unique time-stamps for files processed with the same
     *        user-provided date
     * 
     * @return a populated {@link MetaMedia} object, or {@code null} if the file is not supported
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
        AbstractImageParser parser = ImageParserFactory.getParser(fpath);
        DigitalSignature format = parser.getImageFormat();
        Metadata<?> meta = parser.readMetadata();

        if (meta != null && meta.hasMetadata())
        {
            if (meta instanceof MetadataTIF)
            {
                metadataDate = extractExifDate((MetadataTIF) meta);
            }

            else if (meta instanceof MetadataPNG)
            {
                metadataDate = extractPngDate((MetadataPNG<?>) meta);
            }

            // If metadata exists but no usable date was found
            if (metadataDate == null)
            {
                emptyMetadata = true;
                LOGGER.info("Metadata found, but no date field available for [" + parser.getImageFile() + "]");
            }
        }

        else
        {
            emptyMetadata = true;
            LOGGER.info("No EXIF/metadata date found for [" + parser.getImageFile() + "]");
        }

        FileTime modifiedTime = resolveDateTaken(metadataDate, fpath, attr, userDateTime, dateOffset);

        return new MetaMedia(fpath, modifiedTime, format, emptyMetadata);
    }

    /**
     * Extracts the date from EXIF metadata in a {@link MetadataTIF} object.
     *
     * @param tif
     *        the {@code MetadataTIF} instance
     * 
     * @return a {@code Date} object from the EXIF data, or {@code null} if not found
     */
    private static Date extractExifDate(MetadataTIF tif)
    {
        if (tif.hasExifData())
        {
            DirectoryIFD dir = tif.getDirectory(DirectoryIdentifier.EXIF_DIRECTORY_SUBIFD);

            if (dir != null)
            {
                return dir.getDate(EXIF_TAG_DATE_TIME_ORIGINAL);
            }
        }

        return null;
    }

    /**
     * Extracts the date from PNG metadata. It first checks for embedded EXIF data, then
     * falls back to textual chunks if available.
     *
     * @param png
     *        the {@code MetadataPNG} instance
     * 
     * @return a {@code Date} object from the PNG data, or {@code null} if not found
     */
    private static Date extractPngDate(MetadataPNG<?> png)
    {
        if (png.hasExifData())
        {
            Object dir = png.getDirectory(MetadataTIF.class);

            if (dir instanceof MetadataTIF)
            {
                Date exifDate = extractExifDate((MetadataTIF) dir);

                if (exifDate != null)
                {
                    return exifDate;
                }
            }
        }

        else if (png.hasTextualData())
        {
            Object dir = png.getDirectory(ChunkType.Category.TEXTUAL);

            if (dir instanceof ChunkDirectory)
            {
                List<TextEntry> data = ((ChunkDirectory) dir).getTextualData(TextKeyword.CREATE);

                if (!data.isEmpty())
                {
                    Date parsed = DateParser.convertToDate(data.get(0).getValue());

                    if (parsed != null)
                    {
                        return parsed;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Determines the appropriate {@code Date Taken} time-stamp for an image file based on a set of
     * prioritised sources.
     *
     * <p>
     * The method checks for dates in the following order:
     * </p>
     *
     * <ol>
     * <li>A user-provided date, if available</li>
     * <li>The date from image metadata, for example: EXIF or Textual chunk, if available</li>
     * <li>The file's last modified time-stamp</li>
     * </ol>
     *
     * <p>
     * When a user-provided date is used, a 10-second offset is applied for each file to ensure
     * unique time-stamps, which prevents sorting conflicts.
     * </p>
     *
     * @param metadataDate
     *        the date retrieved from the image's metadata
     * @param fpath
     *        the path to the image file
     * @param attr
     *        the file's basic attributes, used for the fallback last modified time
     * @param userDateTime
     *        a user-defined date string that takes precedence over other sources
     * @param dateOffset
     *        an offset value to create unique time-stamps for user-provided dates
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

            LOGGER.warn("Invalid user date format [" + userDateTime + "]. Falling back to metadata or file timestamp");
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
     * Executes the batch copying process. To be sub-classed to provide an accurate implementation.
     * 
     * <p>
     * This method iterates through the internal sorted set of {@link MetaMedia} objects and copies
     * each source file to the target directory. It renames the copied file using the designated
     * prefix and updates its file time attributes (creation, last modified, and last access) to
     * match the "Date Taken" timestamp determined during the scan phase.
     * </p>
     */
    @Override
    public void updateAndCopyFiles() throws FileNotFoundException, IOException
    {
        this.forEach(System.out::println);
    }
}