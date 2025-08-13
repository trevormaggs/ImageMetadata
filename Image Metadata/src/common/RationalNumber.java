package common;

import java.text.NumberFormat;
import java.util.Objects;

/**
 * This class represents a rational number, typically used in the TIFF image format.
 * 
 * TIFF rational numbers are defined as pairs of 32-bit integers, encompassing numerator and
 * denominator.
 *
 * To overcome Java's lack of support for unsigned types, this class stores the numerator and
 * denominator as 64-bit long integers, implementing necessary masking for the unsigned type.
 *
 * As per the TIFF/EXIF specifications, the use of 32-bit unsigned integers is necessary. As
 * Java does not have an unsigned type, both numerator and denominator variables are declared as
 * long to ensure storage for large numbers, preventing any potential negative numbers.
 * 
 * @author Trevor Maggs
 * @version 1.0
 * @since 13 August 2025
 */
public class RationalNumber extends Number
{
    public enum DataType
    {
        SIGNED, UNSIGNED;
    }

    public final long numerator;
    public final long divisor;
    public final boolean unsignedType;

    /**
     * Computes the greatest common divisor in a recursive fashion.
     * 
     * @param a
     *        the top number of the fraction
     * @param b
     *        the bottom number of the fraction
     * 
     * @return a valid divisible value for both numerator and divisor
     */
    private static long gcd(long a, long b)
    {
        if (b == 0)
        {
            return a;
        }

        return gcd(b, a % b);
    }

    /**
     * Computes and returns the rational number as a double-precision floating-point value.
     *
     * This method performs the division of the numerator by the divisor using double arithmetic
     * to preserve precision. It serves as a common utility method for other numeric conversions
     * such as {@code floatValue()}, {@code doubleValue()}, and {@code intValue()}.
     *
     * @return the floating-point result of numerator divided by divisor
     * 
     * @throws ArithmeticException
     *         if the divisor is zero
     */
    private double getFraction()
    {
        if (divisor == 0)
        {
            throw new ArithmeticException("Denominator cannot be zero");
        }

        return (double) numerator / (double) divisor;
    }

    /**
     * Constructs an instance to handle both signed and unsigned integers.
     *
     * @param num
     *        a numerator in either 32-bit signed or unsigned format
     * @param div
     *        a non-zero divisor in either 32-bit signed or unsigned format
     * @param type
     *        specifies whether the specified values should be interpreted as unsigned or signed.
     *        Acceptable values are either {@code DataType.UNSIGNED} or {@code DataType.SIGNED} only
     */
    public RationalNumber(int num, int div, DataType type)
    {
        this.unsignedType = (type == DataType.UNSIGNED);
        this.numerator = unsignedType ? (num & 0xFFFFFFFFL) : num;
        this.divisor = unsignedType ? (div & 0xFFFFFFFFL) : div;
    }

    /**
     * A private constructor for methods such as negate() that create instances of this class using
     * the content of the current instance.
     *
     * @param num
     *        a numerator in 64-bit long
     * @param div
     *        a non-zero divisor in 64-bit long
     * @param type
     *        indicates whether the specified values are to be treated as unsigned or not. It can
     *        only be either {@code DataType.UNSIGNED} or {@code DataType.SIGNED}
     */
    private RationalNumber(long num, long div, DataType type)
    {
        this.unsignedType = (type == DataType.UNSIGNED);
        this.numerator = num;
        this.divisor = div;
    }

    /**
     * Constructs an instance to store signed integers.
     *
     * @param num
     *        a numerator in either 32-bit signed or unsigned format
     * @param div
     *        a non-zero divisor in either 32-bit signed or unsigned format
     */
    public RationalNumber(int num, int div)
    {
        this(num, div, DataType.SIGNED);
    }

    /**
     * Negates the value of the RationalNumber. If the numerator of this instance has its high-order
     * bit set, then its value is too large to be treated as a Java 32-bit signed integer. In such a
     * case, the only way that a RationalNumber instance can be negated is to divide both terms by a
     * common divisor, if a non-zero common divisor exists. However, if no such divisor exists,
     * there is no numerically correct way to perform the negation. When a negation cannot be
     * performed correctly, this method throws an unchecked exception.
     *
     * @return a valid instance with a negated value.
     */
    public RationalNumber negate()
    {
        long n = numerator;
        long d = divisor;

        /*
         * An instance of an unsigned type can be negated if and only if its high-order bit (the
         * sign bit) is clear. If the bit is set, the value will be too large to convert to a signed
         * type. In such a case it is necessary to adjust the numerator and denominator by their
         * greatest common divisor (gcd), if one exists. no non-zero common divisor exists, an
         * exception is thrown.
         */
        if (unsignedType && n >> 31 == 1)
        {
            /*
             * the unsigned value is so large that the high-order bit is set it cannot be converted
             * to a negative number. Check to see whether there is an option to reduce its
             * magnitude.
             */
            long g = gcd(numerator, divisor);

            if (g != 0)
            {
                n /= g;
                d /= g;
            }

            if (n >> 31 == 1)
            {
                throw new NumberFormatException("Unsigned numerator is too large to negate " + numerator);
            }
        }

        return new RationalNumber(-n, d, DataType.SIGNED);
    }

    /**
     * Returns a string representation after computationally dividing the numerator by the
     * denominator. It is functionally similar to the {@code toString()} method, so maybe this
     * method should be removed after the testing.
     * 
     * @return a String value after the computation
     */
    public String toFormattedString()
    {
        NumberFormat nf = NumberFormat.getInstance();

        nf.setMaximumFractionDigits(3);

        if (numerator % divisor == 0)
        {
            return Long.toString((long) getFraction());

        }

        return nf.format(getFraction());
    }

    /**
     * Returns the integer representation of the rational number. It performs the conversion by
     * dividing the numerator by the denominator and then rounding down to the nearest whole number.
     * Note that this operation may result in a loss of precision due to the rounding process.
     * 
     * @return the integer value after computation
     */
    @Override
    public int intValue()
    {
        return (int) Math.floor(getFraction());
    }

    /**
     * Returns the long representation of the rational number. It performs the conversion by
     * dividing the numerator by the denominator and then rounding down to the nearest whole number.
     * Note that this operation may result in a loss of precision due to the rounding process.
     * 
     * @return the long value after computation
     */
    @Override
    public long longValue()
    {
        return (long) getFraction();
    }

    /**
     * Returns the floating-point representation of the rational number. It obtains a
     * double-precision value during computation to maintain as much precision from the original
     * numerator and denominator as possible.
     *
     * The alternative expression using {@code (float)numerator / (float)denominator} risks losing
     * precision because a Java float type only supports up to 24 bits, whereas an integer supports
     * up to 32 bits.
     *
     * @return the floating-point value after computation
     */
    @Override
    public float floatValue()
    {
        return (float) getFraction();
    }

    /**
     * Returns the double representation of the rational number. It performs the conversion by
     * dividing the numerator by the denominator and then rounding down to the nearest whole number.
     * Note that this operation may result in a loss of precision due to the rounding process.
     *
     * @return the double value after computation
     */
    @Override
    public double doubleValue()
    {
        return getFraction();
    }

    /**
     * Returns a string representation after computationally dividing the numerator by the
     * denominator.
     * 
     * @return a String value after the computation
     */
    @Override
    public String toString()
    {
        NumberFormat nf = NumberFormat.getInstance();

        if (divisor == 0)
        {
            return String.format("Invalid rational number detected (%d/%d)", numerator, divisor);
        }

        if (numerator % divisor == 0)
        {
            return nf.format(getFraction());
        }

        else
        {
            return String.format("%d/%d (%s)", numerator, divisor, nf.format(getFraction()));
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof RationalNumber))
        {
            return false;
        }

        RationalNumber other = (RationalNumber) obj;

        return (numerator == other.numerator &&
                divisor == other.divisor &&
                unsignedType == other.unsignedType);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(numerator, divisor, unsignedType);
    }

    /**
     * Simplifies or reduces large fractions, expressed as rational numbers. It involves finding the
     * greatest common divisor (GCD) and dividing both the numerator and the denominator to yield
     * the fraction in its most simplified form.
     * 
     * @param numerator
     *        The top number of the fraction
     * @param divisor
     *        The bottom number of the fraction
     * @param unsignedType
     *        A boolean flag indicating whether the fraction is unsigned
     * 
     * @return An instance of the RationalNumber class, representing the simplified fraction
     */
    public static RationalNumber simplifyLargeFraction(long numerator, long divisor, boolean unsignedType)
    {
        long gcd;
        long n = numerator;
        long d = divisor;

        if (d < 0)
        {
            n = -n;
            d = -d;
        }

        gcd = gcd(n, d);

        return new RationalNumber(n / gcd, d / gcd, (unsignedType ? DataType.UNSIGNED : DataType.SIGNED));
    }

    // NOT TESTED YET
    public boolean hasIntegerValue()
    {
        return divisor == 1 || (divisor != 0 && (numerator % divisor == 0)) || (divisor == 0 && numerator == 0);
    }

    public String toSimpleString(boolean decimalAllowed)
    {
        if (hasIntegerValue())
        {
            return Integer.toString(intValue());
        }

        else
        {
            RationalNumber simplifiedInstance = simplifyLargeFraction(numerator, divisor, unsignedType);

            if (decimalAllowed)
            {
                String doubleString = Double.toString(simplifiedInstance.doubleValue());

                if (doubleString.length() < 5)
                {
                    return doubleString;
                }
            }

            return simplifiedInstance.toString();
        }
    }
}