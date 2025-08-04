package png;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.zip.InflaterInputStream;
import common.ByteValueConverter;
import logger.LogFactory;

/**
 * Extended to support a zTXt (compressed textual data) chunk in a PNG file.
 *
 * <p>
 * This class provides decoding support for PNG zTXt chunks, which store compressed Latin-1 text
 * paired with a keyword.
 * </p>
 * 
 * @version 1.0
 * @since 28 July 2025
 */
public class PngChunkZTXT extends PngChunk
{
    private static final LogFactory LOGGER = LogFactory.getLogger(PngChunkZTXT.class);
    private final String keyword;
    private final String text;

    /**
     * Constructs a {@code PngChunkZTXT} instance.
     *
     * <p>
     * According to the PNG specification, the chunk format is:
     * </p>
     * 
     * <ul>
     * <li><b>Keyword</b>: 1–79 bytes (Latin-1), followed by a null byte</li>
     * <li><b>Compression method</b>: 1 byte (only value 0 is valid for zlib/deflate)</li>
     * <li><b>Compressed text</b>: remaining bytes, compressed using zlib</li>
     * </ul>
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
    public PngChunkZTXT(long length, byte[] typeBytes, int crc32, byte[] data)
    {
        super(length, typeBytes, crc32, data);

        int keywordPos = 0;
        String parsedKeyword = "";
        String parsedText = "";

        try
        {
            // Find null terminator and extract keyword
            while (keywordPos < payload.length && payload[keywordPos] != 0)
            {
                keywordPos++;
            }

            if (keywordPos == 0 || keywordPos > 79)
            {
                throw new IllegalStateException("Invalid zTXt keyword length (must be 1–79 characters). Found [" + keywordPos + "]");
            }

            parsedKeyword = new String(payload, 0, keywordPos, StandardCharsets.ISO_8859_1);

            // consume null byte
            int pos = keywordPos + 1;

            if (pos >= payload.length)
            {
                throw new IllegalStateException("Malformed zTXt chunk: No compression method byte found");
            }

            int compressionMethod = payload[pos++] & 0xFF;

            if (compressionMethod != 0)
            {
                throw new IllegalStateException("Invalid compression method in zTXt chunk. Expected 0, but found [" + compressionMethod + "]");
            }

            if (pos >= payload.length)
            {
                throw new IllegalStateException("Malformed zTXt chunk: No compressed data present");
            }

            byte[] rawCompressedText = Arrays.copyOfRange(payload, pos, payload.length);

            try (InputStream inflater = new InflaterInputStream(new ByteArrayInputStream(rawCompressedText)))
            {
                byte[] decompressed = ByteValueConverter.readAllBytes(inflater);

                parsedText = new String(decompressed, StandardCharsets.ISO_8859_1);
            }
        }

        catch (IOException | IllegalStateException exc)
        {
            LOGGER.error(exc.getMessage(), exc);

            this.keyword = "";
            this.text = "";

            return;
        }

        this.keyword = parsedKeyword;
        this.text = parsedText;
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
     * Extracts and de-compresses the keyword-text pair from this zTXt chunk.
     *
     * @return an {@link Optional} containing the extracted the keyword and the de-compressed text
     *         as a {@link TextEntry} instance if present, otherwise, {@link Optional#empty()}
     */
    @Override
    public Optional<TextEntry> getKeywordPair()
    {
        if (keyword.isEmpty())
        {
            return Optional.empty();
        }

        return Optional.of(new TextEntry(getTag(), keyword, text));
    }

    /**
     * Gets the keyword extracted from the zTXt chunk.
     *
     * @return the keyword text, or an empty string if parsing failed during construction
     */
    public String getKeyword()
    {
        return keyword;
    }

    /**
     * Gets the decompressed text extracted from the zTXt chunk.
     *
     * @return the decompressed text, or an empty string if parsing or decompression failed during
     *         construction
     */
    public String getText()
    {
        return text;
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

        return line.toString();
    }
}