package cz.metdat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real PostgreSQL (embedded binaries, no Docker needed) with the Flyway schema + seed data. Each test rolls back. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractIntegrationTest {
    static final EmbeddedPostgres PG;
    static {
        try { PG = EmbeddedPostgres.builder().start(); } catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", () -> PG.getJdbcUrl("postgres", "postgres"));
        r.add("spring.datasource.username", () -> "postgres");
        r.add("spring.datasource.password", () -> "postgres");
        r.add("app.storage.type", () -> "database");
        r.add("app.security.jwt-secret", () -> "test-secret-test-secret-test-secret-123456");
    }

    public static final long EDITOR_ID = 1L;   // Adam Novotný
    public static final long READER_ID = 7L;   // Jakub Dvořák

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;

    protected String editor, reader;

    @BeforeEach
    void tokens() throws Exception {
        editor = "Bearer " + login(EDITOR_ID);
        reader = "Bearer " + login(READER_ID);
    }

    protected String login(long userId) throws Exception {
        String body = mvc.perform(post("/api/auth/demo-login").contentType("application/json").content("{\"userId\":" + userId + "}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body).get("token").asText();
    }

    protected static MockMultipartFile pdf(String name) {
        return new MockMultipartFile("files", name, "application/pdf", "%PDF-1.4\n% test\n%%EOF".getBytes(StandardCharsets.UTF_8));
    }

    protected static String testJson(long typeId, String status, String result) {
        return "{\"vin\":\"TMBTST26A00009999\",\"testTypeId\":" + typeId + ",\"department\":\"ESW\",\"testDate\":\"2026-03-11\","
            + "\"responsiblePerson\":\"Adam Novotný\",\"status\":\"" + status + "\","
            + (result == null ? "" : "\"result\":\"" + result + "\",") + "\"comment\":\"automated test\"}";
    }

    /** Creates a test as Editor; returns its JSON. */
    protected JsonNode create(String status, String result, MockMultipartFile... files) throws Exception {
        var req = MockMvcRequestBuilders.multipart("/api/tests")
            .file(new MockMultipartFile("data", "", "application/json", testJson(1, status, result).getBytes(StandardCharsets.UTF_8)));
        for (var f : files) req.file(f);
        String body = mvc.perform(req.header("Authorization", editor)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(body);
    }
}
