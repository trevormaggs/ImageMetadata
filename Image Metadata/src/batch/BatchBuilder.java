package batch;

import java.io.IOException;

/**
 * <p>
 * Implements the Builder design pattern for step-by-step construction of a {@link BatchExecutor}
 * instance. This abstract base class provides setter methods to configure batch parameters, which
 * can then be used to create a new {@code BatchExecutor} using the {@link #build()} method.
 * </p>
 *
 * <p>
 * <b>Example:</b>
 * </p>
 *
 * <pre>
 * <code>
 * Builder batch = new Builder()
 *         .source("D:\\KDR Project\\Milestones\\TestBatch")
 *         .target("local\\images")
 *         .name("image")
 *         .descending(true)
 *         .datetime("26 4 2006")
 *         .build();
 * </code>
 * </pre>
 * 
 * @author Trevor Maggs
 * @since 1.0
 */
public final class BatchBuilder
{
    protected String bd_sourceDir = BatchExecutor.DEFAULT_SOURCE_DIRECTORY;
    protected String bd_prefix = BatchExecutor.DEFAULT_IMAGE_PREFIX;
    protected String bd_target = BatchExecutor.DEFAULT_TARGET_DIRECTORY;
    protected String bd_datetime = "";
    protected boolean bd_descending = false;
    protected boolean bd_embedDateTime = false;
    protected boolean bd_skipVideoFiles = false;
    protected boolean bd_debug = false;
    protected String[] bd_files = new String[0];

    /**
     * Sets the source directory containing original image files.
     *
     * @param src
     *        the source directory, must not be null
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder source(final String src)
    {
        bd_sourceDir = src;
        return this;
    }

    /**
     * Sets a prefix to be prepended to each output image file name.
     * 
     * @param prefix
     *        the name to be appended to every copied file
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder name(final String prefix)
    {
        bd_prefix = prefix;
        return this;
    }

    /**
     * Sets the target directory where copied image files are saved to.
     *
     * @param target
     *        the target directory
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder target(final String target)
    {
        bd_target = target;
        return this;
    }

    /**
     * Sets the date and time to modify the {@code Date Taken} attribute.
     *
     * @param dt
     *        the date and time attribute
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder datetime(final String dt)
    {
        bd_datetime = dt;
        return this;
    }

    /**
     * Enables a flag to sort the copied images in descending order.
     *
     * @param desc
     *        a true boolean value to sort the list in descending order
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder descending(final boolean desc)
    {
        bd_descending = desc;
        return this;
    }

    /**
     * Appends the date and time attribute to each image file name.
     *
     * @param emb
     *        {@code true} to append the date/time to the filename, or {@code false} to exclude it
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder embedDateTime(final boolean emb)
    {
        bd_embedDateTime = emb;
        return this;
    }

    /**
     * Determines whether media files should be ignored or copied.
     *
     * @param media
     *        a boolean true value to skip media files, otherwise copy them
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder skipVideo(final boolean media)
    {
        bd_skipVideoFiles = media;
        return this;
    }

    /**
     * Enables or disables the debug mode.
     *
     * @param debug
     *        a boolean true value to enable debugging, otherwise false to disable it
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder debug(final boolean debug)
    {
        bd_debug = debug;
        return this;
    }

    /**
     * Specifies a list of individual image files to copy, instead of copying all files in the
     * source directory.
     * 
     * @param files
     *        a string array of file names to be copied
     *
     * @return this object to allow method chaining
     */
    public BatchBuilder fileSet(final String[] files)
    {
        bd_files = files;
        return this;
    }

    /**
     *
     * @return a newly created instance of the BatchImageEngine outer class
     *
     * @throws BatchErrorException
     *         in case of an error during batch processing
     * @throws IOException
     */
    public BatchExecutor build() throws BatchErrorException, IOException
    {
        return new BatchExecutor(this);
    }
}