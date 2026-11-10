package cz.metdat.service;

import cz.metdat.domain.TechnicalTest;
import cz.metdat.dto.TestFilter;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** F09: CSV export of ALL records matching the filter (not just one page). RFC 4180 quoting, UTF-8 with BOM, formula-injection safe. */
@Service
public class ExportService {
    private static final String[] HEADER = {"Test ID", "Vehicle ID / VIN", "Test Type", "Test Type Active", "Department", "Test Date",
        "Calendar Week", "Period Start", "Period End", "Responsible Person", "Status", "Result", "Comment", "Attachments",
        "Created By", "Created At", "Modified By", "Modified At"};

    private final TechnicalTestService tests;

    public ExportService(TechnicalTestService tests) { this.tests = tests; }

    public byte[] csv(TestFilter filter, String sort) {
        List<TechnicalTest> rows = tests.findAllMatching(filter, sort);
        Map<Long, List<String>> names = tests.attachmentNames(rows.stream().map(TechnicalTest::getId).toList());
        StringBuilder sb = new StringBuilder("﻿");
        line(sb, List.of(HEADER));
        for (TechnicalTest t : rows) {
            line(sb, List.of(t.getTestNumber(), t.getVin(), t.getTestType().getName(), String.valueOf(t.getTestType().isActive()),
                t.getDepartment(), t.getTestDate() == null ? "" : t.getTestDate().toString(), t.getWeekLabel(),
                t.getPeriodStart().toString(), t.getPeriodEnd().toString(), t.getResponsiblePerson(), t.getStatus().label(),
                t.getResult() == null ? "" : t.getResult().label(), nz(t.getComment()),
                String.join("; ", names.getOrDefault(t.getId(), List.of())),
                nz(t.getCreatedBy()), t.getCreatedAt() == null ? "" : t.getCreatedAt().toString(),
                nz(t.getModifiedBy()), t.getModifiedAt() == null ? "" : t.getModifiedAt().toString()));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    static String cell(String v) {
        String s = v == null ? "" : v;
        if (!s.isEmpty() && "=+-@\t\r".indexOf(s.charAt(0)) >= 0) s = "'" + s;   // spreadsheet formula injection guard
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) s = "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private static void line(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) { if (i > 0) sb.append(','); sb.append(cell(cells.get(i))); }
        sb.append("\r\n");
    }
    private static String nz(String s) { return s == null ? "" : s; }
}
