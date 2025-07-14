package common;

/**
 * This exception is thrown when the running application is terminated abruptly. It can be further
 * examined using the {@link #getCause()} method.
 * 
 * The class encapsulates the standard Java checked exception, enhancing it with customised error
 * handling and reporting capabilities.
 * 
 * <p>
 * Change History:
 * </p>
 * 
 * <ul>
 * <li>Version 1.0 - Initial release by Trevor Maggs on 21 June 2025</li>
 * </ul> 
 * 
 * @version 0.1
 * @author Trevor Maggs, trevmaggs@tpg.com.au
 * @since 21 June 2025
 */
public class ImageReadErrorException extends Exception
{
    private static final long serialVersionUID = -7008757798069486039L;

    /**
     * Constructs a default {@code BatchErrorException} instance without an error message. The
     * underlying cause of the exception is not set initially, but can be specified later by calling
     * the {@link #initCause(Throwable)} method.
     */
    public ImageReadErrorException()
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
    public ImageReadErrorException(String message)
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
    public ImageReadErrorException(Throwable cause)
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
    public ImageReadErrorException(String message, Throwable cause)
    {
        super(message, cause);
    }
}