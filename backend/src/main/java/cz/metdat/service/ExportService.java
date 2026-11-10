package cz.metdat.service;

import cz.metdat.domain.TechnicalTest;
import cz.metdat.dto.TestFilter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * F09: export of ALL records matching the filter (not just one page) as CSV or XLSX. Both formats are built from the same rows,
 * so they always contain identical data. CSV is UTF-8 with BOM so Excel detects the encoding.
 */
@Service
public class ExportService {
    private static final List<String> HEADER = List.of("Test ID", "Vehicle ID / VIN", "Test Type", "Test Type Active", "Department", "Test Date",
        "Calendar Week", "Period Start", "Period End", "Responsible Person", "Status", "Result", "Comment", "Attachments",
        "Created By", "Created At", "Modified By", "Modified At");

    private final TechnicalTestService tests;

    public ExportService(TechnicalTestService tests) { this.tests = tests; }

    public byte[] csv(TestFilter filter, String sort) {
        StringBuilder csv = new StringBuilder("﻿");
        CsvWriter.line(csv, HEADER);
        rows(filter, sort).forEach(row -> CsvWriter.line(csv, row));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] xlsx(TestFilter filter, String sort) {
        return XlsxWriter.write("Technical tests", HEADER, rows(filter, sort));
    }

    private List<List<String>> rows(TestFilter filter, String sort) {
        List<TechnicalTest> matches = tests.findAllMatching(filter, sort);
        Map<Long, List<String>> attachmentNames = tests.attachmentNames(matches.stream().map(TechnicalTest::getId).toList());
        List<List<String>> rows = new ArrayList<>(matches.size());
        for (TechnicalTest t : matches) {
            rows.add(List.of(
                t.getTestNumber(), t.getVin(), t.getTestType().getName(), String.valueOf(t.getTestType().isActive()),
                t.getDepartment(), text(t.getTestDate()), t.getWeekLabel(), text(t.getPeriodStart()), text(t.getPeriodEnd()),
                t.getResponsiblePerson(), t.getStatus().label(), t.getResult() == null ? "" : t.getResult().label(),
                text(t.getComment()), String.join("; ", attachmentNames.getOrDefault(t.getId(), List.of())),
                text(t.getCreatedBy()), text(t.getCreatedAt()), text(t.getModifiedBy()), text(t.getModifiedAt())));
        }
        return rows;
    }

    private static String text(Object value) { return value == null ? "" : value.toString(); }
}
