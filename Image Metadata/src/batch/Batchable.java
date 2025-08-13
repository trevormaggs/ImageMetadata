package batch;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface Batchable
{
    public void copyToTarget() throws FileNotFoundException, IOException;
}