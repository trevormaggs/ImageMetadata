package batch;

import java.util.Comparator;
import java.nio.file.Path;

/**
 * Comparator that sorts {@code MediaFile} objects by timestamp in ascending order. If time-stamps
 * are equal, it falls back to comparing file paths to ensure uniqueness in sorted collections like
 * TreeSet.
 */
class DefaultTimestampComparator implements Comparator<MediaFile>
{
    @Override
    public int compare(MediaFile o1, MediaFile o2)
    {
        int cmp = Long.compare(o1.getTimestamp(), o2.getTimestamp());

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