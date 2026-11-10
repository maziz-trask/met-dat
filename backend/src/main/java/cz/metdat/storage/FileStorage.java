package cz.metdat.storage;

import java.io.IOException;
import java.io.InputStream;

/** Where attachment bytes live. Metadata (and the link to the test) is always in the attachment table. */
public interface FileStorage {
    void store(String key, byte[] data) throws IOException;
    InputStream open(String key) throws IOException;
    boolean exists(String key);
    void delete(String key);
}
