package cz.metdat.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Stores file bytes in PostgreSQL (table attachment_content): persistent on hosts without a writable disk. */
@Component
@ConditionalOnProperty(name = "app.storage.type", havingValue = "database")
public class DatabaseStorage implements FileStorage {
    private final JdbcTemplate jdbc;

    public DatabaseStorage(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void store(String key, byte[] data) {
        jdbc.update("INSERT INTO attachment_content (storage_key, data) VALUES (?, ?) ON CONFLICT (storage_key) DO UPDATE SET data = EXCLUDED.data", key, data);
    }
    @Override public InputStream open(String key) throws IOException {
        List<byte[]> rows = jdbc.query("SELECT data FROM attachment_content WHERE storage_key = ?", (rs, i) -> rs.getBytes(1), key);
        if (rows.isEmpty()) throw new IOException("Attachment content not found");
        return new ByteArrayInputStream(rows.get(0));
    }
    @Override public boolean exists(String key) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM attachment_content WHERE storage_key = ?", Integer.class, key);
        return n != null && n > 0;
    }
    @Override public void delete(String key) { jdbc.update("DELETE FROM attachment_content WHERE storage_key = ?", key); }
}
