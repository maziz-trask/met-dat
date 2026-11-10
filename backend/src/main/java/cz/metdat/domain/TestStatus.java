package cz.metdat.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import cz.metdat.exception.ValidationException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/** Workflow status. Draft/In Progress/Completed are mandatory (spec); Planned/Cancelled come from the dummy workbook. */
public enum TestStatus {
    DRAFT("Draft"), PLANNED("Planned"), IN_PROGRESS("In Progress"), COMPLETED("Completed"), CANCELLED("Cancelled");

    private final String label;

    TestStatus(String label) { this.label = label; }

    @JsonValue
    public String label() { return label; }

    @JsonCreator
    public static TestStatus fromLabel(String value) {
        String v = value == null ? "" : value.trim();
        return Arrays.stream(values()).filter(s -> s.label.equalsIgnoreCase(v)).findFirst()
                .orElseThrow(() -> new ValidationException("status", "Unknown status '" + value + "'"));
    }

    @Converter
    public static class DbConverter implements AttributeConverter<TestStatus, String> {
        @Override public String convertToDatabaseColumn(TestStatus s) { return s == null ? null : s.label; }
        @Override public TestStatus convertToEntityAttribute(String v) { return v == null ? null : fromLabel(v); }
    }
}
