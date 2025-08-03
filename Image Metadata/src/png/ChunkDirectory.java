package png;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import common.Directory;
import png.ChunkType.Category;
import tif.TagEntries.Taggable;

/**
 * This class provides the functionality to maintain chunk data stored in a Directory, grouped by
 * its category.
 * 
 * If the chunks are of the textual type, either
 * {@code ChunkType.tEXt, ChunkType.iTXt or ChunkType.zTXt}, they may be used to extract metadata
 * information if available.
 * 
 * Otherwise, if the eXIf chunk happens to exist, it will also be extracted to provide the metadata
 * information.
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
public class ChunkDirectory implements Directory<PngChunk>
{
    private Category category;
    private List<PngChunk> chunks;

    /**
     * This default constructor should not be invoked, or it will throw an exception to prevent
     * instantiation.
     *
     * @throws UnsupportedOperationException
     *         to indicate that instantiation is not supported
     */
    public ChunkDirectory()
    {
        throw new UnsupportedOperationException("Not intended for instantiation");
    }

    /**
     * Constructs a new directory instance to store a collection of chunks whose category matches
     * with the specified parameter. If any incoming entries do not correspond to this value, they
     * will not be processed.
     *
     * @param category
     *        the type of ChunkType.Category enumeration
     */
    public ChunkDirectory(Category category)
    {
        this.category = category;
        this.chunks = new ArrayList<>();
    }

    /**
     * Returns the category that this directory focuses on.
     *
     * @return a value of ChunkType.Category enumeration
     */
    public Category getDirectoryCategory()
    {
        return category;
    }

    /**
     * Searches through the collection of textual chunks and checks whether a chunk containing the
     * specified keyword has been set. In normal cases, the following chunk types are potentially
     * expected to contain it: either {@code ChunkType.tEXt, ChunkType.iTXt or ChunkType.zTXt}.
     *
     * @param keyword
     *        specifies the TextKeyword enumeration to check
     *
     * @return a boolean value of true to indicate a successful match, otherwise false if no match
     *         is found
     */
    public boolean existsTextualKeyword(TextKeyword keyword)
    {
        for (PngChunk chunk : chunks)
        {
            if (chunk.hasKeywordPair(keyword))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Searches through the collection of textual chunks and returns a list of keyword-value pairs
     * containing the specified keyword if present. In normal cases, the following chunk types are
     * potentially expected to contain it: either
     * {@code ChunkType.tEXt, ChunkType.iTXt or ChunkType.zTXt}.
     *
     * @param keyword
     *        specifies the TextKeyword enumeration
     *
     * @return a list of TextEntry objects. It may be empty if no match is found
     */
    public List<TextEntry> getTextualData(TextKeyword keyword)
    {
        return getTextualData(keyword.getKeyword());
    }

    /**
     * Searches through the collection of textual chunks and returns a list of keyword-value pairs
     * matching the specified keyword. In normal cases, the following chunk types are expected to
     * contain it: either {@code ChunkType.tEXt, ChunkType.iTXt or ChunkType.zTXt}.
     *
     * @param keyword
     *        the keyword in string form to find the corresponding data. Ensure the keyword complies
     *        with the PNG specification detailed in <a href=
     *        "https://www.w3.org/TR/png/#11keywords">https://www.w3.org/TR/png/#11keywords</a>.
     *
     * @return a list of TextEntry objects. It may be empty if no match is found
     */
    public List<TextEntry> getTextualData(String keyword)
    {
        List<TextEntry> keyTextPair = new ArrayList<>();

        for (PngChunk chunk : chunks)
        {
            if (chunk.getType().getCategory() == ChunkType.Category.TEXTUAL && chunk.getKeywordPair().isPresent())
            {
                TextEntry pair = chunk.getKeywordPair().get();

                if (pair == null || !keyword.equals(pair.getKeyword()))
                {
                    continue;
                }

                keyTextPair.add(pair);
            }
        }

        return keyTextPair;
    }

    /**
     * Checks if the list contains a particular chunk. For example, checking if
     * {@code ChunkType.eXIf} has been set.
     *
     * @param chunk
     *        an enumeration value to check the specified chunk
     *
     * @return a boolean value of true if the specified chunk is present
     */
    public boolean hasChunk(ChunkType chunk)
    {
        return (findEntryByID(chunk.getIndexID()) != null);
    }

    /**
     * Adds a new {@code PngChunk} entry to this Directory.
     *
     * @param chunk
     *        {@code PngChunk} object
     *
     * @return true if this collection changed as a result of the call if the tag exists, otherwise
     *         false
     * @throws IllegalArgumentException
     *         if the category of the specified chunk does not agree with the original entry defined
     *         in the constructor
     */
    @Override
    public boolean add(PngChunk chunk)
    {
        if (chunk.getType().getCategory() != category)
        {
            throw new IllegalArgumentException("Inconsistent chunk type detected. The category for this directory must be [" + chunk.getType().getCategory().getDescription() + "]");
        }

        return chunks.add(chunk);
    }

    /**
     * Removes an entry from this Directory.
     *
     * @param chunk
     *        {@code PngChunk} object to remove
     *
     * @return true if this collection changed as a result of the call if the tag exists, otherwise
     *         false
     */
    @Override
    public boolean remove(PngChunk chunk)
    {
        return chunks.remove(chunk);
    }

    /**
     * Indicates whether the specified tag has been set.
     *
     * @param tag
     *        used to search for a match
     *
     * @return true if the tag exists in this Directory, otherwise false
     */
    @Override
    public boolean contains(Taggable tag)
    {
        return (findEntryByID(tag.getNumberID()) != null);
    }

    /**
     * Returns a {@code PngChunk} entry based on the specified ID.
     *
     * @param id
     *        the ID identifying the object
     *
     * @return the {@code PngChunk} entry, or null if no matching entry is found
     */
    @Override
    public PngChunk findEntryByID(int id)
    {
        for (PngChunk chunk : chunks)
        {
            if (chunk.getType().getIndexID() == id)
            {
                return chunk;
            }
        }

        return null;
    }

    /**
     * Returns the length of elements present in this Directory.
     *
     * @return the total number of elements
     */
    @Override
    public int length()
    {
        return chunks.size();
    }

    /**
     * Retrieves an iterator to navigate through a collection of {@code PngChunk} objects.
     *
     * @return an Iterator object
     */
    @Override
    public Iterator<PngChunk> iterator()
    {
        return chunks.iterator();
    }

    /**
     * Generates a string representation of the chunk entries.
     *
     * @return a formatted string
     */
    @Override
    public String toString()
    {
        StringBuilder line = new StringBuilder();

        Iterator<PngChunk> iter = iterator();

        while (iter.hasNext())
        {
            line.append(iter.next());
            line.append(System.lineSeparator());
        }

        return line.toString();
    }
}