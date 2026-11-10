package cz.metdat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.metdat.domain.TestResult;
import cz.metdat.domain.TestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Create/update payload of a technical test. Audit fields are deliberately absent: they are server-generated. */
public record TestRequest(
    @NotBlank(message = "{validation.vin.required}")
    @Pattern(regexp = "^[A-HJ-NPR-Za-hj-npr-z0-9]{17}$", message = "{validation.vin.format}")
    String vin,
    @NotNull(message = "{validation.testType.required}") Long testTypeId,
    @NotBlank(message = "{validation.department.required}") @Size(max = 20, message = "{validation.department.size}") String department,
    /** Exact date (optional if testWeek is given). */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate testDate,
    /** ISO calendar week in workbook notation KTww/yy (optional if testDate is given). */
    @Pattern(regexp = "^(KT\\d{2}/\\d{2})?$", message = "{validation.week.format}") String testWeek,
    @NotBlank(message = "{validation.person.required}") @Size(max = 150, message = "{validation.person.size}") String responsiblePerson,
    @NotNull(message = "{validation.status.required}") TestStatus status,
    TestResult result,
    @Size(max = 5000, message = "{validation.comment.size}") String comment,
    /** Optional optimistic-lock version from the last read. */
    Long version) {}
