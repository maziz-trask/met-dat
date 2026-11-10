package cz.metdat.service;

import java.util.List;

/** Minimal RFC 4180 CSV encoding with spreadsheet formula-injection protection. */
public final class CsvWriter {
    private CsvWriter() {}

    public static String cell(String value) {
        String s = value == null ? "" : value;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;   // would be executed as a formula by Excel
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    public static void line(StringBuilder out, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) out.append(',');
            out.append(cell(cells.get(i)));
        }
        out.append("\r\n");
    }
}
