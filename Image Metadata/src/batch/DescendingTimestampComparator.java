package batch;

import java.nio.file.Path;
import java.util.Comparator;

/**
 * Comparator that sorts MetaMedia objects by timestamp in descending order. If time-stamps are
 * equal, it falls back to comparing file paths to ensure uniqueness in sorted collections like
 * TreeSet.
 */
class DescendingTimestampComparator implements Comparator<MetaMedia>
{
    @Override
    public int compare(MetaMedia o1, MetaMedia o2)
    {
        int cmp = Long.compare(o2.getTimestamp(), o1.getTimestamp());

        if (cmp != 0)
        {
            return cmp;
        }

        // Fallback to path comparison to avoid equality on identical time-stamps
        Path p1 = o1.getPath();
        Path p2 = o2.getPath();

        if (p1 == null && p2 == null)
        {
            return 0;
        }

        else if (p1 == null)
        {
            return -1;
        }

        else if (p2 == null)
        {
            return 1;
        }

        return p1.compareTo(p2);
    }
}