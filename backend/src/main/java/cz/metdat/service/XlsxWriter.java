package cz.metdat.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Writes a single-sheet .xlsx (Office Open XML) without external libraries: bold shaded header row, frozen header, auto-filter,
 * sized columns. All values are written as inline TEXT cells, so spreadsheet formulas in the data can never execute.
 */
public final class XlsxWriter {
    private static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final int MIN_WIDTH = 10, MAX_WIDTH = 60;

    private XlsxWriter() {}

    public static byte[] write(String sheetName, List<String> header, List<List<String>> rows) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ZipOutputStream zip = new ZipOutputStream(bytes, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", contentTypes());
            put(zip, "_rels/.rels", rootRels());
            put(zip, "xl/workbook.xml", workbook(sheetName));
            put(zip, "xl/_rels/workbook.xml.rels", workbookRels());
            put(zip, "xl/styles.xml", styles());
            put(zip, "xl/worksheets/sheet1.xml", sheet(header, rows));
            zip.finish();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not build XLSX", e);
        }
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypes() {
        return XML + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/></Types>";
    }

    private static String rootRels() {
        return XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>";
    }

    private static String workbook(String sheetName) {
        return XML + "<workbook xmlns=\"" + NS + "\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"" + escape(safeSheetName(sheetName)) + "\" sheetId=\"1\" r:id=\"rId1\"/></sheets></workbook>";
    }

    private static String workbookRels() {
        return XML + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/></Relationships>";
    }

    /** Style 0 = normal text, style 1 = bold header with light blue fill. */
    private static String styles() {
        return XML + "<styleSheet xmlns=\"" + NS + "\">"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font><font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill><fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFD9E2F3\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/></cellXfs></styleSheet>";
    }

    private static String sheet(List<String> header, List<List<String>> rows) {
        int columns = header.size();
        StringBuilder xml = new StringBuilder(XML).append("<worksheet xmlns=\"").append(NS).append("\">");
        xml.append("<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>");
        xml.append("<cols>");
        for (int c = 0; c < columns; c++) {
            xml.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1).append("\" width=\"").append(width(header, rows, c)).append("\" customWidth=\"1\"/>");
        }
        xml.append("</cols><sheetData>");
        appendRow(xml, 1, header, 1);
        for (int r = 0; r < rows.size(); r++) appendRow(xml, r + 2, rows.get(r), 0);
        xml.append("</sheetData>");
        xml.append("<autoFilter ref=\"A1:").append(columnName(columns - 1)).append(rows.size() + 1).append("\"/></worksheet>");
        return xml.toString();
    }

    private static void appendRow(StringBuilder xml, int rowNumber, List<String> cells, int style) {
        xml.append("<row r=\"").append(rowNumber).append("\">");
        for (int c = 0; c < cells.size(); c++) {
            String value = cells.get(c);
            if (value == null || value.isEmpty()) continue;   // empty cell
            xml.append("<c r=\"").append(columnName(c)).append(rowNumber).append("\" t=\"inlineStr\"");
            if (style != 0) xml.append(" s=\"").append(style).append('"');
            xml.append("><is><t xml:space=\"preserve\">").append(escape(value)).append("</t></is></c>");
        }
        xml.append("</row>");
    }

    private static int width(List<String> header, List<List<String>> rows, int column) {
        int longest = header.get(column).length();
        for (List<String> row : rows) {
            String v = row.get(column);
            if (v != null) longest = Math.max(longest, v.lines().mapToInt(String::length).max().orElse(0));
        }
        return Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, longest + 2));
    }

    /** 0 -> A, 25 -> Z, 26 -> AA ... */
    static String columnName(int index) {
        StringBuilder name = new StringBuilder();
        for (int i = index; i >= 0; i = i / 26 - 1) name.insert(0, (char) ('A' + i % 26));
        return name.toString();
    }

    private static String safeSheetName(String name) {
        String cleaned = name.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        return cleaned.isEmpty() ? "Sheet1" : cleaned.substring(0, Math.min(31, cleaned.length()));
    }

    /** XML 1.0 escaping; characters that are illegal in XML 1.0 are dropped. */
    static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 8);
        s.codePoints().forEach(cp -> {
            boolean legal = cp == 0x9 || cp == 0xA || cp == 0xD || (cp >= 0x20 && cp <= 0xD7FF) || (cp >= 0xE000 && cp <= 0xFFFD) || (cp >= 0x10000 && cp <= 0x10FFFF);
            if (!legal) return;
            switch (cp) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                default -> out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }
}
