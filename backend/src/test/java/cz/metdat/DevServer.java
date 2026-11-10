package cz.metdat;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;

/**
 * Docker-free local runner: starts a persistent embedded PostgreSQL (data in ./data/pg) and the application against it.
 * Run via scripts/run-local.ps1. For real deployments use Docker Compose or an external PostgreSQL instead.
 */
public class DevServer {
    public static void main(String[] args) throws Exception {
        EmbeddedPostgres pg = EmbeddedPostgres.builder()
            .setDataDirectory(Path.of("data/pg")).setCleanDataDirectory(false).setPort(Integer.getInteger("dev.pg.port", 5433)).start();
        System.setProperty("spring.datasource.url", pg.getJdbcUrl("postgres", "postgres"));
        System.setProperty("spring.datasource.username", "postgres");
        System.setProperty("spring.datasource.password", "postgres");
        System.setProperty("app.security.jwt-secret", System.getProperty("app.security.jwt-secret", "dev-only-secret-dev-only-secret-0123456789"));
        SpringApplication.run(MetDatApplication.class, args);
    }
}
