package cz.metdat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import cz.metdat.service.XlsxWriter;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/** F09 XLSX: all filtered rows, well-formed OOXML package, safe text cells. */
class XlsxExportTest extends AbstractIntegrationTest {
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static Map<String, byte[]> unzip(byte[] xlsx) throws Exception {
        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(xlsx))) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) entries.put(e.getName(), zip.readAllBytes());
        }
        return entries;
    }

    private static Document xml(byte[] bytes) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
    }

    private static int rowCount(Map<String, byte[]> entries) throws Exception {
        NodeList rows = xml(entries.get("xl/worksheets/sheet1.xml")).getElementsByTagNameNS("*", "row");
        return rows.getLength();
    }

    @Test @DisplayName("XLSX export has every matching row (not one page), a header row and a valid OOXML package")
    void allRowsAndValidPackage() throws Exception {
        byte[] all = mvc.perform(get("/api/tests/export?format=xlsx&size=5").header("Authorization", reader)).andExpect(status().isOk())
            .andExpect(header().string("Content-Type", XLSX))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".xlsx")))
            .andReturn().getResponse().getContentAsByteArray();
        Map<String, byte[]> parts = unzip(all);
        assertThat(parts.keySet()).contains("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml");
        parts.values().forEach(p -> { try { xml(p); } catch (Exception e) { throw new AssertionError("part is not well-formed XML", e); } });
        assertThat(rowCount(parts)).isEqualTo(51);   // header + 50 seeded tests
        String sheet = new String(parts.get("xl/worksheets/sheet1.xml"), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(sheet).contains("Test ID", "TEST-2026-0001", "TMBTST26A00000001", "Jakub Dvořák");

        Map<String, byte[]> completed = unzip(mvc.perform(get("/api/tests/export?format=xlsx&status=Completed").header("Authorization", reader))
            .andReturn().getResponse().getContentAsByteArray());
        assertThat(rowCount(completed)).isEqualTo(29);   // header + 28 Completed
        Map<String, byte[]> csvEquivalent = unzip(mvc.perform(get("/api/tests/export?format=XLSX&dateFrom=2026-02-16&dateTo=2026-02-22").header("Authorization", reader))
            .andReturn().getResponse().getContentAsByteArray());
        assertThat(new String(csvEquivalent.get("xl/worksheets/sheet1.xml"), java.nio.charset.StandardCharsets.UTF_8)).contains("TEST-2026-0001");
    }

    @Test @DisplayName("Export needs a signed-in user; unknown formats are rejected with a readable message")
    void securityAndFormat() throws Exception {
        mvc.perform(get("/api/tests/export?format=xlsx")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tests/export?format=pdf").header("Authorization", reader)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("xlsx")));
        mvc.perform(get("/api/tests/export?format=pdf").header("Authorization", reader).header("Accept-Language", "cs")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Podporované formáty")));
    }

    @Test @DisplayName("XlsxWriter keeps formulas as text, escapes XML and drops illegal characters")
    void writerSafety() throws Exception {
        byte[] bytes = XlsxWriter.write("Sheet: <bad>/name?", List.of("A", "B"), List.of(List.of("=SUM(A1:A9)", "x < y & \"z\" \u0001\u0008 ok"), List.of("", "ř é ü 漢")));
        Map<String, byte[]> parts = unzip(bytes);
        String sheet = new String(parts.get("xl/worksheets/sheet1.xml"), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(sheet).contains("t=\"inlineStr\"").contains(">=SUM(A1:A9)<").contains("x &lt; y &amp; &quot;z&quot;  ok").contains("ř é ü 漢").doesNotContain("<f>");
        xml(parts.get("xl/worksheets/sheet1.xml"));
        xml(parts.get("xl/workbook.xml"));
        assertThat(new String(parts.get("xl/workbook.xml"), java.nio.charset.StandardCharsets.UTF_8)).contains("name=\"Sheet  &lt;bad&gt; name\"");
        assertThat(rowCount(parts)).isEqualTo(3);
    }
}
