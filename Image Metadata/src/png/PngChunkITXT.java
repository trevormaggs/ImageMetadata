package png;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.InflaterInputStream;
import common.ByteValueConverter;
import logger.LogFactory;

/**
 * Extended to support an {@code iTXt} chunk in a PNG file, which stores international text data.
 *
 * This chunk supports both compressed and uncompressed UTF-8 encoded text, along with optional
 * language and translated keyword metadata.
 *
 * @version 1.0
 * @since 28 July 2025
 */
public class PngChunkITXT extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkITXT.class);
    private String keyword;
    private String text;
    private String languageTag;
    private String translatedKeyword;

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
    }

    /**
     * Parses and decodes the {@code iTXt} chunk, extracting the keyword and its associated
     * UTF-8 encoded text content.
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
     * @return an {@link Optional} containing the extracted keyword and text as a {@link TextEntry}
     *         instance if present, otherwise, {@link Optional#empty()}
     *
     * @throws IllegalStateException
     *         if the structure is malformed or decompression fails
     */
    @Override
    public Optional<TextEntry> getKeywordPair()
    {
        int pos;
        byte[] data = getDataArray();

        try
        {
            // Read to length of keyword from offset 0
            keyword = ByteValueConverter.readNullTerminatedString(data, 0, StandardCharsets.ISO_8859_1);

            pos = keyword.length() + 1;

            if (pos > 80)
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

                // Read one byte after compressionFlag
                int compressionMethod = data[pos++] & 0xFF;

                if (compressionFlag == 1 && compressionMethod != 0)
                {
                    throw new IllegalStateException("Invalid iTXt compression method. Expected 0. Found: [" + compressionMethod + "]");
                }

                // Read to length of language after compressionMethod
                languageTag = ByteValueConverter.readNullTerminatedString(data, pos, StandardCharsets.ISO_8859_1);
                pos += languageTag.length() + 1;

                // Read to length of Translated keyword after languageTag plus one null character
                translatedKeyword = ByteValueConverter.readNullTerminatedString(data, pos, StandardCharsets.UTF_8);
                pos += translatedKeyword.length() + 1;

                // Text field (compressed or uncompressed, UTF-8)
                if (compressionFlag == 1)
                {
                    byte[] compressed = Arrays.copyOfRange(data, pos, data.length);

                    try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressed)))
                    {
                        byte[] decompressed = ByteValueConverter.readAllBytes(inflater);
                        text = new String(decompressed, StandardCharsets.UTF_8);
                    }
                }

                else
                {
                    text = new String(data, pos, data.length - pos, StandardCharsets.UTF_8);
                }

                return Optional.of(new TextEntry(getTag(), keyword, text));
            }

            else
            {
                throw new IllegalStateException("Unexpected end of chunk data detected");
            }
        }

        catch (IOException | IllegalStateException exc)
        {
            LOGGER.error("Failed to parse iTXt chunk (type: [" + getType().getChunkName() + "], length: [" + getLength() + "])", exc);
        }

        return Optional.empty();
    }

    /**
     * Gets the keyword extracted from the iTXt chunk. <b>Note</b>, the {@link #getKeywordPair()}
     * method must be called first before calling this method to ensure data integrity.
     *
     * @return the keyword or null if not yet parsed
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Gets the text extracted from the iTXt chunk. <b>Note</b>, the {@link #getKeywordPair()}
     * method must be called first before calling this method to ensure data integrity.
     *
     * @return the UTF-8 text or null if not yet parsed
     */
    public String getText()
    {
        return text;
    }

    /**
     * Gets the language tag extracted from the iTXt chunk. <b>Note</b>, the
     * {@link #getKeywordPair()} method must be called first before calling this method to ensure
     * data integrity.
     *
     * @return the language tag or null if not yet parsed
     */
    public String getLanguageTag()
    {
        return languageTag;
    }

    /**
     * Gets the translated keyword extracted from the iTXt chunk. <b>Note</b>, the
     * {@link #getKeywordPair()} method must be called first before calling this method to ensure
     * data integrity.
     *
     * @return the translated keyword or null if not yet parsed
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