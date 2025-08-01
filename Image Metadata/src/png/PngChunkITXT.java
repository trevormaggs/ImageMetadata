package png;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.InflaterInputStream;
import common.ByteValueConverter;
import logger.LogFactory;

/**
 * Extended to support an {@code iTXt} chunk in a PNG file, which stores international text data.
 *
 * This chunk supports both compressed and uncompressed UTF-8 encoded text, along with optional
 * language and translated keyword metadata.
 *
 * <p>
 * The iTXt chunk layout consists of:
 * </p>
 *
 * <ul>
 * <li>Keyword (Latin-1): 1–79 bytes + null terminator</li>
 * <li>Compression flag: 1 byte (0 = uncompressed, 1 = compressed)</li>
 * <li>Compression method: 1 byte (must be 0 for zlib/deflate)</li>
 * <li>Language tag (Latin-1): null-terminated string</li>
 * <li>Translated keyword (UTF-8): null-terminated string</li>
 * <li>Text (UTF-8): compressed or plain text depending on the compression flag</li>
 * </ul>
 *
 * @version 1.0
 * @since 28 July 2025
 */
public class PngChunkITXT extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkITXT.class);
    private final String keyword;
    private final String text;
    private final String languageTag;
    private final String translatedKeyword;

    /**
     * Constructs a new {@code PngChunkITXT} with the specified parameters.
     *
     * @param length
     *        the length of the chunk's data field (excluding type and CRC)
     * @param typeBytes
     *        the raw 4-byte chunk type
     * @param crc32
     *        the CRC value read from the file
     * @param data
     *        raw chunk data
     */
    public PngChunkITXT(int length, byte[] typeBytes, int crc32, byte[] data)
    {
        super(length, typeBytes, crc32, data);

        int pos = 0;
        String parsedKeyword;
        String parsedText;
        String parsedLanguage;
        String parsedTranslated;

        try
        {
            // Read to length of keyword from offset 0
            parsedKeyword = ByteValueConverter.readNullTerminatedString(data, 0, StandardCharsets.ISO_8859_1);
            pos = parsedKeyword.length() + 1;

            if (parsedKeyword.isEmpty() || pos > 80)
            {
                throw new IllegalStateException("Invalid iTXt keyword length (must be 1–79 characters)");
            }

            else if (pos < data.length)
            {
                // Read one byte after length of keyword plus one null character
                int compressionFlag = data[pos++] & 0xFF;

                if (compressionFlag != 0 && compressionFlag != 1)
                {
                    throw new IllegalStateException("Invalid compression flag in iTXt: expected 0 (uncompressed) or 1 (compressed). Found: [" + compressionFlag + "]");
                }

                /*
                 * Read one byte after compressionFlag
                 *
                 * Compression method is always present (even if compression flag == 0),
                 * but only used when compressed
                 */
                int compressionMethod = data[pos++] & 0xFF;

                if (compressionFlag == 1 && compressionMethod != 0)
                {
                    throw new IllegalStateException("Invalid iTXt compression method. Expected 0. Found: [" + compressionMethod + "]");
                }

                // Read to length of language after compressionMethod
                parsedLanguage = ByteValueConverter.readNullTerminatedString(data, pos, StandardCharsets.ISO_8859_1);
                pos += parsedLanguage.length() + 1;

                // Read to length of Translated keyword after languageTag plus one null character
                parsedTranslated = ByteValueConverter.readNullTerminatedString(data, pos, StandardCharsets.UTF_8);
                pos += parsedTranslated.getBytes(StandardCharsets.UTF_8).length + 1;

                // Text field (compressed or uncompressed, UTF-8)
                if (compressionFlag == 1)
                {
                    byte[] compressed = Arrays.copyOfRange(data, pos, data.length);

                    try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressed)))
                    {
                        byte[] decompressed = ByteValueConverter.readAllBytes(inflater);
                        parsedText = new String(decompressed, StandardCharsets.UTF_8);
                    }
                }

                else
                {
                    parsedText = new String(data, pos, data.length - pos, StandardCharsets.UTF_8);
                }
            }

            else
            {
                throw new IllegalStateException("Unexpected end of chunk data detected");
            }
        }

        catch (IOException | IllegalStateException exc)
        {
            LOGGER.error(exc.getMessage() + ". Payload: [" + ByteValueConverter.toHex(payload) + "]", exc);

            this.keyword = "";
            this.text = "";
            this.languageTag = "";
            this.translatedKeyword = "";

            return;
        }

        this.keyword = parsedKeyword;
        this.text = parsedText;
        this.languageTag = parsedLanguage;
        this.translatedKeyword = parsedTranslated;
    }

    /**
     * Checks whether this chunk contains the specified textual keyword.
     *
     * @param keyword
     *        the {@link TextKeyword} to search for
     *
     * @return true if found, false otherwise
     *
     * @throws IllegalArgumentException
     *         if the specified keyword is null
     */
    @Override
    public boolean hasKeywordPair(TextKeyword keyword)
    {
        if (keyword == null || keyword.getKeyword() == null)
        {
            throw new IllegalArgumentException("Keyword cannot be null");
        }

        return keyword.getKeyword().equals(this.keyword);
    }

    /**
     * Gets the keyword extracted from the iTXt chunk.
     *
     * @return the keyword
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Gets the text extracted from the iTXt chunk.
     *
     * @return the UTF-8 text
     */
    public String getText()
    {
        return text;
    }

    /**
     * Gets the language tag extracted from the iTXt chunk.
     *
     * @return the language tag
     */
    public String getLanguageTag()
    {
        return languageTag;
    }

    /**
     * Gets the translated keyword extracted from the iTXt chunk.
     *
     * @return the translated keyword
     */
    public String getTranslatedKeyword()
    {
        return translatedKeyword;
    }

    /**
     * Returns a string representation of the chunk's properties and contents.
     *
     * @return a formatted string describing this chunk
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        line.append(super.toString());
        line.append(String.format(" %-20s %s%n", "[Keyword]", getKeyword()));
        line.append(String.format(" %-20s %s%n", "[Text]", getText()));
        line.append(String.format(" %-20s %s%n", "[Translated Keyword]", getTranslatedKeyword()));
        line.append(String.format(" %-20s %s%n", "[Language Tag]", getLanguageTag()));

        return line.toString();
    }
}