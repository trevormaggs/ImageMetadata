package batch;

/**
 * This exception is thrown when the running application is terminated abruptly. It can be further
 * examined using the {@link #getCause()} method.
 * 
 * The class encapsulates the standard Java checked exception, enhancing it with customised error
 * handling and reporting capabilities.
 * 
 * @author Trevor Maggs
 * @since 11 July 2024
 */
class BatchErrorException extends Exception
{
    private static final long serialVersionUID = -7008757798069486039L;

    /**
     * Constructs a default {@code BatchErrorException} instance without an error message. The
     * underlying cause of the exception is not set initially, but can be specified later by calling
     * the {@link #initCause(Throwable)} method.
     */
    public BatchErrorException()
    {
    }

    /**
     * Constructs a {@code BatchErrorException} instance with the specified error message. The
     * underlying cause of the exception is not set initially, but can be specified later by calling
     * the {@link #initCause(Throwable)} method.
     * 
     * @param message
     *        the error message, which can be retrieved later using the {@link #getMessage()} method
     */
    public BatchErrorException(String message)
    {
        super(message);
    }

    /**
     * Constructs a {@code BatchErrorException} instance with the specified cause. The detail error
     * message is set to:
     * 
     * <pre>
     * (cause == null ? null : cause.toString())
     * </pre>
     * 
     * This typically contains the class and the detail error message associated with the specified
     * {@code cause}.
     * 
     * @param cause
     *        the underlying cause of this exception, which can be retrieved later using the
     *        {@link #getCause()} method. A null value is permitted, indicating that the cause is
     *        either non-existent or unknown
     */
    public BatchErrorException(Throwable cause)
    {
        super(cause);
    }

    /**
     * Constructs a {@code BatchErrorException} instance with the specified detail error message and
     * the cause.
     * 
     * @param message
     *        the detail error message, which can be retrieved later using the {@link #getMessage()}
     *        method
     * @param cause
     *        the underlying cause of this exception, which can be retrieved later using the
     *        {@link #getCause()} method. A null value is permitted, indicating that the cause is
     *        either non-existent or unknown
     */
    public BatchErrorException(String message, Throwable cause)
    {
        super(message, cause);
    }
}