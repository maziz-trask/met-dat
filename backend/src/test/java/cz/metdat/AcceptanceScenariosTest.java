package cz.metdat;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** The 22 critical acceptance scenarios of the assignment, executed through the real HTTP/security stack against PostgreSQL. */
class AcceptanceScenariosTest extends AbstractIntegrationTest {

    private String text(MvcResult r) throws Exception { return r.getResponse().getContentAsString(StandardCharsets.UTF_8); }

    private void changeStatus(long id, String status, String auth, int expected) throws Exception {
        mvc.perform(patch("/api/tests/" + id + "/status").header("Authorization", auth).contentType("application/json")
            .content("{\"status\":\"" + status + "\"}")).andExpect(status().is(expected));
    }

    private void upload(long id, MockMultipartFile f, String auth, int expected) throws Exception {
        mvc.perform(MockMvcRequestBuilders.multipart("/api/tests/" + id + "/attachments").file(f).header("Authorization", auth)).andExpect(status().is(expected));
    }

    @Test @DisplayName("1 Reader can view a test")
    void s01() throws Exception {
        mvc.perform(get("/api/tests/1").header("Authorization", reader)).andExpect(status().isOk())
            .andExpect(jsonPath("$.testNumber").value("TEST-2026-0001")).andExpect(jsonPath("$.attachments.length()").value(1));
        mvc.perform(get("/api/tests").header("Authorization", reader)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(50));
    }

    @Test @DisplayName("2 Reader can download an attachment (synthetic seed file)")
    void s02() throws Exception {
        String att = text(mvc.perform(get("/api/tests/4/attachments").header("Authorization", reader)).andExpect(status().isOk()).andReturn());
        long attId = json.readTree(att).get(0).get("id").asLong();
        MvcResult r = mvc.perform(get("/api/tests/4/attachments/" + attId + "/download").header("Authorization", reader))
            .andExpect(status().isOk()).andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment"))).andReturn();
        assertThat(new String(r.getResponse().getContentAsByteArray(), StandardCharsets.ISO_8859_1)).startsWith("%PDF-").contains("SYNTHETIC DEMO FILE");
    }

    @Test @DisplayName("3 Reader cannot create a test")
    void s03() throws Exception {
        mvc.perform(MockMvcRequestBuilders.multipart("/api/tests").file(new MockMultipartFile("data", "", "application/json", testJson(1, "Draft", null).getBytes()))
            .header("Authorization", reader)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        // authorisation is decided before payload validation: an invalid body from a Reader is still 403, not 400
        mvc.perform(MockMvcRequestBuilders.multipart("/api/tests").file(new MockMultipartFile("data", "", "application/json", "{}".getBytes()))
            .header("Authorization", reader)).andExpect(status().isForbidden());
    }

    @Test @DisplayName("4 Reader cannot edit a test (PUT and status PATCH)")
    void s04() throws Exception {
        mvc.perform(put("/api/tests/1").header("Authorization", reader).contentType("application/json").content(testJson(1, "Draft", null))).andExpect(status().isForbidden());
        changeStatus(1, "Draft", reader, 403);
    }

    @Test @DisplayName("5 Reader cannot upload an attachment")
    void s05() throws Exception { upload(1, pdf("x.pdf"), reader, 403); }

    @Test @DisplayName("6 Reader cannot delete an attachment")
    void s06() throws Exception {
        mvc.perform(delete("/api/tests/1/attachments/1").header("Authorization", reader)).andExpect(status().isForbidden());
    }

    @Test @DisplayName("7 Editor can create a technical test, audit set by server")
    void s07() throws Exception {
        JsonNode t = create("Draft", null);
        assertThat(t.get("testNumber").asText()).startsWith("TEST-").matches("TEST-\\d{4}-\\d{4}");
        assertThat(t.get("createdBy").asText()).isEqualTo("adam.novotny@example.com");
        assertThat(t.get("createdAt").isNull()).isFalse();
        assertThat(t.get("modifiedBy").isNull()).isTrue();
    }

    @Test @DisplayName("7b Create validates mandatory fields and VIN format on the backend")
    void s07b() throws Exception {
        String bad = "{\"vin\":\"SHORT\",\"department\":\"\",\"responsiblePerson\":\"\"}";
        mvc.perform(MockMvcRequestBuilders.multipart("/api/tests").file(new MockMultipartFile("data", "", "application/json", bad.getBytes()))
            .header("Authorization", editor)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(4)));
    }

    @Test @DisplayName("8+21 Editor can edit; Modified By/At change, Created By/At are preserved")
    void s08_s21() throws Exception {
        JsonNode before = json.readTree(text(mvc.perform(get("/api/tests/2").header("Authorization", editor)).andReturn()));
        String body = testJson(1, "In Progress", "Preliminary").replace("automated test", "edited comment");
        JsonNode after = json.readTree(text(mvc.perform(put("/api/tests/2").header("Authorization", editor).contentType("application/json").content(body))
            .andExpect(status().isOk()).andReturn()));
        assertThat(after.get("comment").asText()).isEqualTo("edited comment");
        assertThat(after.get("modifiedBy").asText()).isEqualTo("adam.novotny@example.com");
        assertThat(after.get("modifiedAt").asText()).isNotEqualTo(before.get("modifiedAt").asText());
        assertThat(after.get("createdBy").asText()).isEqualTo(before.get("createdBy").asText());
        assertThat(after.get("createdAt").asText()).isEqualTo(before.get("createdAt").asText());
        assertThat(after.get("testNumber").asText()).isEqualTo("TEST-2026-0002");
    }

    @Test @DisplayName("9 Editor can manage test types; Reader cannot")
    void s09() throws Exception {
        String created = text(mvc.perform(post("/api/test-types").header("Authorization", editor).contentType("application/json")
            .content("{\"name\":\"Crash test\",\"description\":\"d\"}")).andExpect(status().isCreated()).andExpect(jsonPath("$.active").value(true)).andReturn());
        long id = json.readTree(created).get("id").asLong();
        mvc.perform(put("/api/test-types/" + id).header("Authorization", editor).contentType("application/json").content("{\"name\":\"Crash test v2\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Crash test v2"));
        mvc.perform(patch("/api/test-types/" + id + "/deactivate").header("Authorization", editor)).andExpect(jsonPath("$.active").value(false));
        mvc.perform(patch("/api/test-types/" + id + "/activate").header("Authorization", editor)).andExpect(jsonPath("$.active").value(true));
        mvc.perform(post("/api/test-types").header("Authorization", reader).contentType("application/json").content("{\"name\":\"Nope\"}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/test-types/1/deactivate").header("Authorization", reader)).andExpect(status().isForbidden());
        mvc.perform(post("/api/test-types").header("Authorization", editor).contentType("application/json").content("{\"name\":\"crash TEST v2\"}"))
            .andExpect(status().isBadRequest());   // duplicate name, case-insensitive
    }

    @Test @DisplayName("10 Inactive test type cannot be newly selected (create and edit)")
    void s10() throws Exception {
        long legacy = 11;   // seeded inactive legacy type
        mvc.perform(MockMvcRequestBuilders.multipart("/api/tests").file(new MockMultipartFile("data", "", "application/json", testJson(legacy, "Draft", null).getBytes()))
            .header("Authorization", editor)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("inactive")));
        mvc.perform(put("/api/tests/2").header("Authorization", editor).contentType("application/json").content(testJson(legacy, "Draft", null))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/test-types?activeOnly=true").header("Authorization", reader)).andExpect(jsonPath("$.length()").value(10));
    }

    @Test @DisplayName("11 Existing test with a deactivated type still displays and can be edited keeping that type")
    void s11() throws Exception {
        long typeId = json.readTree(text(mvc.perform(get("/api/tests/1").header("Authorization", reader)).andReturn())).get("testTypeId").asLong();
        mvc.perform(patch("/api/test-types/" + typeId + "/deactivate").header("Authorization", editor)).andExpect(status().isOk());
        mvc.perform(get("/api/tests/1").header("Authorization", reader)).andExpect(status().isOk())
            .andExpect(jsonPath("$.testTypeActive").value(false)).andExpect(jsonPath("$.testTypeName").value("EMC zkouška"));
        mvc.perform(get("/api/tests?testTypeId=" + typeId).header("Authorization", reader)).andExpect(jsonPath("$.content[0].testTypeActive").value(false));
        mvc.perform(put("/api/tests/1").header("Authorization", editor).contentType("application/json")
            .content(testJson(typeId, "Completed", "Failed").replace("automated test", "keeps inactive type"))).andExpect(status().isOk());
    }

    @Test @DisplayName("12 Passed + attachment -> Completed")
    void s12() throws Exception {
        long id = create("In Progress", "Passed", pdf("r.pdf")).get("id").asLong();
        changeStatus(id, "Completed", editor, 200);
        mvc.perform(get("/api/tests/" + id).header("Authorization", reader)).andExpect(jsonPath("$.status").value("Completed"));
    }

    @Test @DisplayName("13 Failed + attachment -> Completed")
    void s13() throws Exception {
        long id = create("In Progress", "Failed", pdf("r.pdf")).get("id").asLong();
        changeStatus(id, "Completed", editor, 200);
    }

    @Test @DisplayName("14 No attachment -> cannot be Completed (409 with exact reason)")
    void s14() throws Exception {
        long id = create("In Progress", "Passed").get("id").asLong();
        mvc.perform(patch("/api/tests/" + id + "/status").header("Authorization", editor).contentType("application/json").content("{\"status\":\"Completed\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COMPLETION_REQUIREMENTS_NOT_MET"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("at least one attachment")));
        mvc.perform(get("/api/tests/" + id).header("Authorization", reader)).andExpect(jsonPath("$.status").value("In Progress"));
    }

    @Test @DisplayName("15 Not evaluated -> cannot be Completed")
    void s15() throws Exception {
        long id = create("In Progress", "Not evaluated", pdf("r.pdf")).get("id").asLong();
        mvc.perform(patch("/api/tests/" + id + "/status").header("Authorization", editor).contentType("application/json").content("{\"status\":\"Completed\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Passed or Failed")));
    }

    @Test @DisplayName("16 Unsupported / empty result -> cannot be Completed (also on create and PUT)")
    void s16() throws Exception {
        for (String r : new String[] {"Conditionally passed", "Passed with remarks", "Preliminary", "Cancelled", null}) {
            long id = create("In Progress", r, pdf("r.pdf")).get("id").asLong();
            changeStatus(id, "Completed", editor, 409);
            mvc.perform(put("/api/tests/" + id).header("Authorization", editor).contentType("application/json").content(testJson(1, "Completed", r))).andExpect(status().isConflict());
        }
        // creating directly as Completed
        mvc.perform(MockMvcRequestBuilders.multipart("/api/tests").file(new MockMultipartFile("data", "", "application/json", testJson(1, "Completed", "Preliminary").getBytes()))
            .file(pdf("r.pdf")).header("Authorization", editor)).andExpect(status().isConflict());
        // an unknown result value is a plain validation error
        mvc.perform(put("/api/tests/2").header("Authorization", editor).contentType("application/json").content(testJson(1, "Draft", "Banana"))).andExpect(status().isBadRequest());
    }

    @Test @DisplayName("16b Creating directly as Completed works with Passed + file")
    void s16b() throws Exception { create("Completed", "Passed", pdf("r.pdf")); }

    @Test @DisplayName("17 Completed test can be reopened to Draft or In Progress")
    void s17() throws Exception {
        long id = create("Completed", "Passed", pdf("r.pdf")).get("id").asLong();
        changeStatus(id, "Draft", editor, 200);
        changeStatus(id, "Completed", editor, 200);
        changeStatus(id, "In Progress", editor, 200);
    }

    @Test @DisplayName("18+19 Last attachment of Completed test cannot be deleted; after reopening it can")
    void s18_s19() throws Exception {
        JsonNode t = create("Completed", "Failed", pdf("only.pdf"));
        long id = t.get("id").asLong(), att = t.get("attachments").get(0).get("id").asLong();
        mvc.perform(delete("/api/tests/" + id + "/attachments/" + att).header("Authorization", editor))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("LAST_ATTACHMENT_OF_COMPLETED_TEST"));
        // with a second attachment one of them may go, the last one may not
        upload(id, pdf("second.pdf"), editor, 201);
        mvc.perform(delete("/api/tests/" + id + "/attachments/" + att).header("Authorization", editor)).andExpect(status().isNoContent());
        long left = json.readTree(text(mvc.perform(get("/api/tests/" + id + "/attachments").header("Authorization", editor)).andReturn())).get(0).get("id").asLong();
        mvc.perform(delete("/api/tests/" + id + "/attachments/" + left).header("Authorization", editor)).andExpect(status().isConflict());
        changeStatus(id, "In Progress", editor, 200);
        mvc.perform(delete("/api/tests/" + id + "/attachments/" + left).header("Authorization", editor)).andExpect(status().isNoContent());
    }

    @Test @DisplayName("20 Export contains ALL filtered rows, not one page; respects filters")
    void s20() throws Exception {
        String all = text(mvc.perform(get("/api/tests/export?format=csv&size=5").header("Authorization", reader)).andExpect(status().isOk())
            .andExpect(header().string("Content-Type", org.hamcrest.Matchers.startsWith("text/csv"))).andReturn());
        assertThat(all.split("\r\n")).hasSize(51);   // header + 50 records
        String completed = text(mvc.perform(get("/api/tests/export?status=Completed").header("Authorization", reader)).andReturn());
        assertThat(completed.split("\r\n")).hasSize(29);
        String range = text(mvc.perform(get("/api/tests/export?dateFrom=2026-02-16&dateTo=2026-02-22").header("Authorization", reader)).andReturn());
        assertThat(range).contains("TEST-2026-0001");
        String list = text(mvc.perform(get("/api/tests?status=Completed&size=5").header("Authorization", reader)).andReturn());
        assertThat(json.readTree(list).get("content")).hasSize(5);
        assertThat(json.readTree(list).get("totalElements").asInt()).isEqualTo(28);
    }

    @Test @DisplayName("22 Unauthenticated / forged direct API calls are rejected")
    void s22() throws Exception {
        mvc.perform(get("/api/tests")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/tests/1/attachments/1/download")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/test-types").contentType("application/json").content("{\"name\":\"x\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tests").header("Authorization", "Bearer not.a.token")).andExpect(status().isUnauthorized());
        // a token with a role claim tampered with is rejected (signature check)
        String[] p = login(READER_ID).split("\\.");
        String forged = new String(java.util.Base64.getUrlDecoder().decode(p[1]), StandardCharsets.UTF_8).replace("READER", "EDITOR");
        String tampered = p[0] + "." + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(forged.getBytes(StandardCharsets.UTF_8)) + "." + p[2];
        mvc.perform(patch("/api/tests/1/status").header("Authorization", "Bearer " + tampered).contentType("application/json").content("{\"status\":\"Draft\"}"))
            .andExpect(status().isUnauthorized());
        // role header / query injection has no effect
        mvc.perform(post("/api/test-types").header("Authorization", reader).header("X-Role", "EDITOR").param("role", "EDITOR")
            .contentType("application/json").content("{\"name\":\"x\"}")).andExpect(status().isForbidden());
    }

    @Test @DisplayName("Attachment validation: unsupported extension, wrong MIME, fake PDF, path traversal name")
    void attachmentValidation() throws Exception {
        upload(1, new MockMultipartFile("files", "evil.exe", "application/octet-stream", "MZ".getBytes()), editor, 400);
        upload(1, new MockMultipartFile("files", "a.pdf", "image/png", "%PDF-1.4".getBytes()), editor, 400);
        upload(1, new MockMultipartFile("files", "a.pdf", "application/pdf", "not a pdf".getBytes()), editor, 400);
        upload(1, new MockMultipartFile("files", "", "application/pdf", new byte[0]), editor, 400);
        String res = text(mvc.perform(MockMvcRequestBuilders.multipart("/api/tests/1/attachments")
            .file(new MockMultipartFile("files", "..\\..\\secret/evil.pdf", "application/pdf", "%PDF-1.4".getBytes())).header("Authorization", editor))
            .andExpect(status().isCreated()).andReturn());
        assertThat(json.readTree(res).get(0).get("originalFilename").asText()).isEqualTo("evil.pdf");
        mvc.perform(MockMvcRequestBuilders.multipart("/api/tests/1/attachments")
            .file(new MockMultipartFile("files", "m.csv", "text/csv", "a,b\n1,2".getBytes())).header("Authorization", editor)).andExpect(status().isCreated());
        upload(999999, pdf("x.pdf"), editor, 404);
    }

    @Test @DisplayName("Server-side sorting + pagination + VIN filter + legacy Completed rows remain editable")
    void searchSortPaging() throws Exception {
        mvc.perform(get("/api/tests?sort=vin,desc&size=3").header("Authorization", reader)).andExpect(jsonPath("$.content[0].vin").value("TMBTST26A00000050"))
            .andExpect(jsonPath("$.totalPages").value(17));
        mvc.perform(get("/api/tests?vin=a0000001").header("Authorization", reader)).andExpect(jsonPath("$.totalElements").value(10));
        mvc.perform(get("/api/tests?sort=password,asc").header("Authorization", reader)).andExpect(status().isBadRequest());
        // legacy Completed record 2 (Conditionally passed): comment-only edit keeps working, F06 is not weakened for the result change
        mvc.perform(put("/api/tests/2").header("Authorization", editor).contentType("application/json")
            .content(testJson(4, "Completed", "Conditionally passed").replace("testTypeId\":4", "testTypeId\":4"))).andExpect(status().isOk());
        mvc.perform(put("/api/tests/2").header("Authorization", editor).contentType("application/json")
            .content(testJson(4, "Completed", "Preliminary"))).andExpect(status().isConflict());
    }
}
