package cz.metdat.service;

import cz.metdat.domain.TechnicalTest;
import cz.metdat.dto.TestFilter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** F09: CSV export of ALL records matching the filter (not just one page). UTF-8 with BOM so Excel detects the encoding. */
@Service
public class ExportService {
    private static final List<String> HEADER = List.of("Test ID", "Vehicle ID / VIN", "Test Type", "Test Type Active", "Department", "Test Date",
        "Calendar Week", "Period Start", "Period End", "Responsible Person", "Status", "Result", "Comment", "Attachments",
        "Created By", "Created At", "Modified By", "Modified At");

    private final TechnicalTestService tests;

    public ExportService(TechnicalTestService tests) { this.tests = tests; }

    public byte[] csv(TestFilter filter, String sort) {
        List<TechnicalTest> rows = tests.findAllMatching(filter, sort);
        Map<Long, List<String>> attachmentNames = tests.attachmentNames(rows.stream().map(TechnicalTest::getId).toList());
        StringBuilder csv = new StringBuilder("﻿");
        CsvWriter.line(csv, HEADER);
        for (TechnicalTest t : rows) {
            CsvWriter.line(csv, List.of(
                t.getTestNumber(), t.getVin(), t.getTestType().getName(), String.valueOf(t.getTestType().isActive()),
                t.getDepartment(), text(t.getTestDate()), t.getWeekLabel(), text(t.getPeriodStart()), text(t.getPeriodEnd()),
                t.getResponsiblePerson(), t.getStatus().label(), t.getResult() == null ? "" : t.getResult().label(),
                text(t.getComment()), String.join("; ", attachmentNames.getOrDefault(t.getId(), List.of())),
                text(t.getCreatedBy()), text(t.getCreatedAt()), text(t.getModifiedBy()), text(t.getModifiedAt())));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String text(Object value) { return value == null ? "" : value.toString(); }
}
