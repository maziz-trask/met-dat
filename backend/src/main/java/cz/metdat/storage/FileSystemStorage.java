package cz.metdat.storage;

import cz.metdat.config.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Stores files below a configured root (needs a persistent volume in production). Keys are server generated and path-checked. */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "filesystem", matchIfMissing = true)
public class FileSystemStorage implements FileStorage {
    private final Path root;

    public FileSystemStorage(AppProperties props) {
        this.root = Path.of(props.storage().path()).toAbsolutePath().normalize();
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) throw new SecurityException("Illegal storage key");   // path traversal guard
        return p;
    }

    @Override public void store(String key, byte[] data) throws IOException {
        Path p = resolve(key);
        Files.createDirectories(p.getParent());
        Files.write(p, data);
    }
    @Override public InputStream open(String key) throws IOException { return Files.newInputStream(resolve(key)); }
    @Override public boolean exists(String key) { return Files.isRegularFile(resolve(key)); }
    @Override public void delete(String key) {
        try { Files.deleteIfExists(resolve(key)); } catch (IOException ignored) { /* orphan file is harmless */ }
    }
}
