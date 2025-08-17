package batch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import common.CommandLineParser;
import common.ProjectBuildInfo;
import common.cli.CommandLineReader;
import logger.LogFactory;

/**
 * <p>
 * This is the main console batch executor used for copying media files and updating metadata.
 * Command line arguments are read and processed, aiming to write copied files to a target directory
 * and sorting them by their {@code Date Taken} metadata attribute.
 * </p>
 *
 * <p>
 * Specifically, it updates each file's creation date, last modification time, and last access
 * time to align with the corresponding {@code Date Taken} property. The sorted list can be either
 * in an ascending (default) or descending chronological order.
 * </p>
 *
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public final class BatchConsole extends BatchExecutor
{
    private static final LogFactory LOGGER = LogFactory.getLogger(BatchConsole.class);
    private static final SimpleDateFormat DF = new SimpleDateFormat("_ddMMMyyyy");
    
    /**
     * Constructs a console interface, using a Builder design pattern to process the parameters and
     * update the copied image files. The actual Builder implementation exists in the
     * {@link BatchBuilder} class.
     *
     * @param builder
     *        the Builder object containing parameters for constructing this instance
     *
     * @throws BatchErrorException
     *         if an I/O or metadata-related error occurs
     * @throws IOException
     *         if an I/O error occurs during reading or writing the image
     */
    public BatchConsole(BatchBuilder builder) throws BatchErrorException, IOException
    {
        super(builder);

        start();
        updateAndCopyFiles();
    }

    /**
     * Iterates through media files and copies them to the target directory.
     * Updates metadata for JPEG and PNG files and logs skipped or empty files.
     *
     * @throws IOException
     *         if an I/O error occurs during reading or writing the image
     */
    @Override
    public void updateAndCopyFiles() throws IOException
    {
        int k = 0;
        Path copied;
        FileTime captureTime;

        for (MediaFile media : this)
        {
            String originalFileName = media.getPath().getFileName().toString();
            String fileExtension = media.getMediaFormat().getFileExtensionName();
            String fname;

            k++;

//            ConsoleBar.updateProgressBar(k, getImageCount());

            if (media.isVideoFormat())
            {
                if (skipVideoFiles())
                {
                    LOGGER.info("File [" + media.getPath() + "] skipped");
                    continue;
                }

                fname = originalFileName.toLowerCase();
                LOGGER.info("File [" + media.getPath() + "] is a video media type. Copied only");
            }

            else
            {
                fname = String.format("%s%d%s.%s", getPrefix(), k, (embedDateTime() ? DF.format(media.getTimestamp()) : ""), fileExtension);
            }

            copied = getTargetDirectory().resolve(fname);
            captureTime = FileTime.fromMillis(media.getTimestamp());

            if (media.isMetadataEmpty())
            {
                if (media.isJPG())
                {
                    BatchMetadataUtils.updateDateTakenMetadataJPG(media.getPath().toFile(), copied.toFile(), captureTime);
                }

                else if (media.isTIF())
                {
                    BatchMetadataUtils.updateDateTakenMetadataTIF(media.getPath().toFile(), copied.toFile(), captureTime);
                }

                else if (media.isPNG())
                {
                    BatchMetadataUtils.updateDateTakenTextualPNG(media.getPath().toFile(), copied.toFile(), captureTime);
                }

                else Files.copy(media.getPath(), copied, StandardCopyOption.COPY_ATTRIBUTES);
            }

            else
            {
                Files.copy(media.getPath(), copied, StandardCopyOption.COPY_ATTRIBUTES);
            }
            
            System.err.printf("%s\t%s\n", media.getPath(), copied);

            BatchMetadataUtils.changeFileTimeProperties(copied, captureTime);
        }
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
            cli.addRule("-p", CommandLineParser.ARG_OPTIONAL);
            cli.addRule("-t", CommandLineParser.ARG_OPTIONAL);
            cli.addRule("-e", CommandLineParser.ARG_BLANK);
            cli.addRule("-k", CommandLineParser.ARG_BLANK);
            cli.addRule("--desc", CommandLineParser.ARG_BLANK);
            cli.addRule("-m", CommandLineParser.ARG_OPTIONAL);
            cli.addRule("-f", CommandLineParser.SEP_OPTIONAL);
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
                System.out.printf("Build date: %s%n", ProjectBuildInfo.getInstance(BatchConsole.class).getBuildDate());
                System.exit(0);
            }

            if (cli.existsOption("-d") || cli.existsOption("--debug"))
            {
                LOGGER.setDebug(true);
            }
        }

        catch (Exception exc)
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
        System.out.format("Usage: %s [-p label] [-t target directory] [-e] [-k] [-m date taken] [-f <File 1> ... <File n>] [--desc] [-d|--debug] [-v|--version] [-h|--help] <Source Directory>%n",
                ProjectBuildInfo.getInstance(BatchConsole.class).getShortFileName());
    }

    /**
     * Prints detailed usage help information, providing guidance on how to use this program.
     */
    private static void showHelp()
    {
        showUsage();
        System.out.println("\nOptions:");
        System.out.println("  -p <prefix>        Prepend copied files with specified prefix");
        System.out.println("  -t <directory>     Target directory where copied files are saved");
        System.out.println("  -e                 Embed date and time in copied file names");
        System.out.println("  -m <date>          Modify file's 'Date Taken' metadata property if empty");
        System.out.println("  -f <files...>      Comma-separated list of specific file names to process");
        System.out.println("  -k                 Skip media files (videos, etc)");
        System.out.println("  --desc             Sort the images in descending order");
        System.out.println("  -v                 Display last build date");
        System.out.println("  -h                 Display this help message and exit");
        System.out.println("  -d                 Enable debugging");
    }

    /**
     * Begins the execution process by reading arguments from the command line and processing them.
     *
     * @param arguments
     *        an array of strings containing the command line arguments
     *
     * @return an instance of BatchConsole
     *
     * @throws BatchErrorException
     *         in case of an error during batch processing
     * @throws IOException
     *         if an I/O error occurs during reading or writing the image
     */
    static BatchConsole readCommand(String[] arguments) throws BatchErrorException, IOException
    {
        CommandLineReader cli = scanArguments(arguments);

        BatchBuilder batch = new BatchBuilder()
                .source(cli.getFirstStandaloneArgument())
                .target(cli.getValueByOption("-t"))
                .name(cli.getValueByOption("-p"))
                .descending(cli.existsOption("--desc"))
                .datetime(cli.getValueByOption("-m"))
                .embedDateTime(cli.existsOption("-e"))
                .skipVideo(cli.existsOption("-k"));

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

        return new BatchConsole(batch);
    }

    public static void main(String[] args) throws Exception
    {
        BatchConsole.readCommand(args);
    }
}