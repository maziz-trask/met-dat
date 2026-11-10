package cz.metdat.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("app")
public record AppProperties(Demo demo, Security security, Cors cors, Storage storage, Upload upload) {
    /** Demo mode: fixed seeded identities, role always read from the database. */
    public record Demo(boolean enabled) {}
    public record Security(String jwtSecret, Duration tokenTtl) {}
    public record Cors(List<String> allowedOrigins) {}
    public record Storage(String type, String path) {}
    public record Upload(DataSize maxFileSize, List<String> allowedExtensions) {}
}
