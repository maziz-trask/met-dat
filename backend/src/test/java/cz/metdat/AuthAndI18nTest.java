package cz.metdat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Registration, password login, role administration and request-language handling. */
class AuthAndI18nTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    private static String email() { return "user-" + UUID.randomUUID() + "@example.com"; }

    private JsonNode register(String email, String password) throws Exception {
        String body = "{\"name\":\"New User\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
        String res = mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return json.readTree(res);
    }

    @Test @DisplayName("Registration creates a Reader whose password is stored only as a BCrypt hash")
    void registrationCreatesReader() throws Exception {
        String email = email();
        JsonNode res = register(email, "S3cret-pass");
        assertThat(res.get("user").get("role").asText()).isEqualTo("READER");
        String hash = jdbc.queryForObject("select password_hash from app_user where email = ?", String.class, email);
        assertThat(hash).startsWith("$2").doesNotContain("S3cret-pass");
        String token = "Bearer " + res.get("token").asText();
        mvc.perform(get("/api/tests").header("Authorization", token)).andExpect(status().isOk());
        mvc.perform(post("/api/test-types").header("Authorization", token).contentType("application/json").content("{\"name\":\"x\"}")).andExpect(status().isForbidden());
    }

    @Test @DisplayName("A role in the registration payload is ignored: nobody can self-register as Editor")
    void roleInPayloadIgnored() throws Exception {
        String res = mvc.perform(post("/api/auth/register").contentType("application/json")
            .content("{\"name\":\"Sneaky\",\"email\":\"" + email() + "\",\"password\":\"longenough1\",\"role\":\"EDITOR\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(json.readTree(res).get("user").get("role").asText()).isEqualTo("READER");
    }

    @Test @DisplayName("Registration validates input and rejects duplicate e-mails (case-insensitive)")
    void registrationValidation() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"name\":\"\",\"email\":\"nope\",\"password\":\"short\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.length()").value(3));
        String email = email();
        register(email, "longenough1");
        mvc.perform(post("/api/auth/register").contentType("application/json")
            .content("{\"name\":\"Dup\",\"email\":\"" + email.toUpperCase() + "\",\"password\":\"longenough1\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
    }

    @Test @DisplayName("Password login: success, wrong password, unknown user and demo identities all behave correctly")
    void passwordLogin() throws Exception {
        String email = email();
        register(email, "longenough1");
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"longenough1\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty()).andExpect(jsonPath("$.user.role").value("READER"));
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"nobody@example.com\",\"password\":\"longenough1\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid e-mail or password."));
        // seeded demo identities have no password: they cannot be used for password login
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"adam.novotny@example.com\",\"password\":\"anything123\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("Repeated failed logins are throttled")
    void loginThrottling() throws Exception {
        String email = email();
        register(email, "longenough1");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"bad-password\"}")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{\"email\":\"" + email + "\",\"password\":\"longenough1\"}"))
            .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"));
    }

    @Test @DisplayName("Auth config and demo users: only password-less seeded identities are listed")
    void configAndDemoUsers() throws Exception {
        register(email(), "longenough1");
        mvc.perform(get("/api/auth/config")).andExpect(jsonPath("$.demoEnabled").value(true)).andExpect(jsonPath("$.registrationEnabled").value(true));
        mvc.perform(get("/api/auth/demo-users")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10));
    }

    @Test @DisplayName("Editors administer roles: promotion takes effect immediately for the SAME token; Readers and self-change are refused")
    void roleAdministration() throws Exception {
        JsonNode res = register(email(), "longenough1");
        long id = res.get("user").get("id").asLong();
        String token = "Bearer " + res.get("token").asText();
        mvc.perform(post("/api/test-types").header("Authorization", token).contentType("application/json").content("{\"name\":\"Promoted " + id + "\"}")).andExpect(status().isForbidden());

        mvc.perform(get("/api/users").header("Authorization", reader)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/users/" + id + "/role").header("Authorization", reader).contentType("application/json").content("{\"role\":\"EDITOR\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").header("Authorization", editor)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(11)));

        mvc.perform(patch("/api/users/" + id + "/role").header("Authorization", editor).contentType("application/json").content("{\"role\":\"EDITOR\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("EDITOR"));
        mvc.perform(post("/api/test-types").header("Authorization", token).contentType("application/json").content("{\"name\":\"Promoted " + id + "\"}")).andExpect(status().isCreated());

        mvc.perform(patch("/api/users/" + EDITOR_ID + "/role").header("Authorization", editor).contentType("application/json").content("{\"role\":\"READER\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SELF_ROLE_CHANGE"));
        mvc.perform(patch("/api/users/" + id + "/role").header("Authorization", editor).contentType("application/json").content("{\"role\":\"READER\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/test-types").header("Authorization", token).contentType("application/json").content("{\"name\":\"Demoted " + id + "\"}")).andExpect(status().isForbidden());
    }

    @Test @DisplayName("API messages follow Accept-Language (cs/en); unsupported languages fall back to English")
    void localisedMessages() throws Exception {
        mvc.perform(post("/api/auth/login").header("Accept-Language", "cs-CZ").contentType("application/json").content("{\"email\":\"x@example.com\",\"password\":\"y\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Neplatný e-mail nebo heslo."));
        mvc.perform(get("/api/tests")).andExpect(jsonPath("$.message").value(containsString("Authentication required")));
        mvc.perform(get("/api/tests").header("Accept-Language", "cs")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value(containsString("přihlášení")));
        mvc.perform(get("/api/tests").header("Accept-Language", "de")).andExpect(jsonPath("$.message").value(containsString("Authentication required")));

        var bad = MockMvcRequestBuilders.multipart("/api/tests").file(new MockMultipartFile("data", "", "application/json", "{\"vin\":\"SHORT\"}".getBytes()))
            .header("Authorization", editor).header("Accept-Language", "cs");
        mvc.perform(bad).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(containsString("VIN musí mít přesně 17")));

        // F06 reason in Czech, with the result label translated through MsgRef
        long id = create("In Progress", "Preliminary", pdf("a.pdf")).get("id").asLong();
        mvc.perform(patch("/api/tests/" + id + "/status").header("Authorization", editor).header("Accept-Language", "cs").contentType("application/json").content("{\"status\":\"Completed\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COMPLETION_REQUIREMENTS_NOT_MET"))
            .andExpect(jsonPath("$.message").value(containsString("výsledek musí být Passed nebo Failed")));
    }
}
