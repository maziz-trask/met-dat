package cz.metdat.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import cz.metdat.exception.ValidationException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;

/** Result. Only PASSED and FAILED count as "evaluated" for the F06 completion rule. */
public enum TestResult {
    PASSED("Passed"), FAILED("Failed"), NOT_EVALUATED("Not evaluated"),
    CONDITIONALLY_PASSED("Conditionally passed"), PASSED_WITH_REMARKS("Passed with remarks"),
    PRELIMINARY("Preliminary"), CANCELLED("Cancelled");

    private final String label;

    TestResult(String label) { this.label = label; }

    @JsonValue
    public String label() { return label; }

    public boolean satisfiesCompletion() { return this == PASSED || this == FAILED; }

    @JsonCreator
    public static TestResult fromLabel(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        return Arrays.stream(values()).filter(s -> s.label.equalsIgnoreCase(v)).findFirst()
                .orElseThrow(() -> new ValidationException("result", "Unknown result '" + value + "'"));
    }

    @Converter
    public static class DbConverter implements AttributeConverter<TestResult, String> {
        @Override public String convertToDatabaseColumn(TestResult r) { return r == null ? null : r.label; }
        @Override public TestResult convertToEntityAttribute(String v) { return v == null ? null : fromLabel(v); }
    }
}
