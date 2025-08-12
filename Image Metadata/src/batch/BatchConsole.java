package batch;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import common.CommandLineParser;
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
public final class BatchConsole extends BatchExecutor
{
    private static final LogFactory LOGGER = LogFactory.getLogger(BatchConsole.class);

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
    public BatchConsole(BatchBuilder builder) throws BatchErrorException
    {
        super(builder);

        scan();
        copyToTarget();
    }

    @Override
    public void copyToTarget()
    {
        int k = 0;
        Path copied;
        FileTime captureTime;
        SimpleDateFormat df = new SimpleDateFormat("_ddMMMyyyy");

        for (MetaMedia media : this)
        {
            // ConsoleBar.updateProgressBar(k, getImageCount());

            String originalFileName = media.getPath().getFileName().toString();
            String fileExtension = getFileExtension(originalFileName);
            String fname;

            k++;

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
                fname = String.format("%s%d%s.%s", getPrefix(), k, (embedDateTime() ? df.format(media.getTimestamp()) : ""), fileExtension);
            }

            copied = getTargetDirectory().resolve(fname);
            captureTime = FileTime.fromMillis(media.getTimestamp());

            if (media.isMetadataEmpty())
            {
                System.err.printf("%s\t%s\n", copied, media.getMediaFormat());
            }

            else if ((media.isJPG() || media.isTIF()) && !media.isMetadataEmpty())
            {

            }

            else if (media.isPNG() && !media.isMetadataEmpty())
            {
            }

            else
            {
            }

            // System.err.printf("%s\n", media);
        }
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

        // Naming & Output
        System.out.println("  -p <prefix>        Prepend copied files with specified prefix");
        System.out.println("  -t <directory>     Target directory where copied files are saved");
        System.out.println("  -e                 Embed date and time in copied file names");

        // Metadata & Processing
        System.out.println("  -m <date>          Modify file's 'Date Taken' metadata property if empty");
        System.out.println("  -f <files...>      Comma-separated list of specific file names to process");
        System.out.println("  -k                 Skip media files (videos, etc)");
        System.out.println("  --desc             Sort the images in descending order");

        // General & Info
        System.out.println("  -v                 Display last build date");
        System.out.println("  -h                 Display this help message and exit");
        System.out.println("  -d                 Enable debugging");
        System.out.println("  --debug            Same as -d");
        System.out.println("  --version          Same as -v");
        System.out.println("  --help             Same as -h");

        System.out.println("\nExamples:");
        System.out.printf("  %s -p tahiti -t newdir C:\\Photos\\holidays%n", ProjectBuildInfo.getInstance(BatchConsole.class).getShortFileName());
        System.out.println("      Copies images from 'C:\\Photos\\holidays' to 'newdir', renaming them with prefix 'tahiti'\n");

        System.out.printf("  %s -m \"26 4 2006\" -f=img1.jpg,img2.jpg C:\\Photos%n", ProjectBuildInfo.getInstance(BatchConsole.class).getShortFileName());
        System.out.println("      Updates 'Date Taken' metadata for specific files if missing\n");
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
    private static BatchConsole readCommand(String[] arguments) throws BatchErrorException, IOException
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
        // String source = "/home/tmaggs/MyJava/TestBatch";
        // -l trev -m "26 4 2006" -i=image2.jpg, image3.jpg "D:\KDR Project\Milestones\TestBatch"
        // -p trev "D:\KDR Project\Milestones\TestBatch" -f=pool1.jpg, pool2.jpg,pool7.jpg
        // -l misty -m "7 10 2012" img

        BatchConsole.readCommand(args);
    }
}