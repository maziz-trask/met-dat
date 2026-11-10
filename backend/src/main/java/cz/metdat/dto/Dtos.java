package cz.metdat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.metdat.domain.TestResult;
import cz.metdat.domain.TestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Request/response objects of the REST API. */
public final class Dtos {
    private Dtos() {}

    public record TestRequest(
        @NotBlank(message = "Vehicle ID / VIN is required")
        @Pattern(regexp = "^[A-HJ-NPR-Za-hj-npr-z0-9]{17}$", message = "VIN must be exactly 17 letters/digits (I, O, Q are not allowed)")
        String vin,
        @NotNull(message = "Test type is required") Long testTypeId,
        @NotBlank(message = "Department is required") @Size(max = 20, message = "Department is at most 20 characters") String department,
        /** Exact date (optional if testWeek is given). */
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate testDate,
        /** ISO calendar week in workbook notation KTww/yy (optional if testDate is given). */
        @Pattern(regexp = "^(KT\\d{2}/\\d{2})?$", message = "Calendar week must look like KT08/26") String testWeek,
        @NotBlank(message = "Responsible person is required") @Size(max = 150, message = "Responsible person is at most 150 characters") String responsiblePerson,
        @NotNull(message = "Status is required") TestStatus status,
        TestResult result,
        @Size(max = 5000, message = "Comment is at most 5000 characters") String comment,
        /** Optional optimistic-lock version from the last read. */
        Long version) {}

    public record StatusChangeRequest(@NotNull(message = "Status is required") TestStatus status) {}

    public record AttachmentResponse(Long id, String originalFilename, String contentType, long fileSize, boolean synthetic,
                                     String uploadedBy, Instant uploadedAt) {}

    public record TestSummary(Long id, String testNumber, String vin, Long testTypeId, String testTypeName, boolean testTypeActive,
                              String department, LocalDate testDate, String testWeek, LocalDate periodStart, LocalDate periodEnd,
                              String responsiblePerson, TestStatus status, TestResult result, long attachmentCount) {}

    public record TestDetail(Long id, String testNumber, String vin, Long testTypeId, String testTypeName, boolean testTypeActive,
                             String department, LocalDate testDate, String testWeek, LocalDate periodStart, LocalDate periodEnd,
                             String responsiblePerson, TestStatus status, TestResult result, String comment,
                             List<AttachmentResponse> attachments, String createdBy, Instant createdAt,
                             String modifiedBy, Instant modifiedAt, long version) {}

    public record TestTypeRequest(
        @NotBlank(message = "Name is required") @Size(max = 150, message = "Name is at most 150 characters") String name,
        @Size(max = 1000, message = "Description is at most 1000 characters") String description,
        Boolean active) {}

    public record TestTypeResponse(Long id, String code, String name, String description, boolean active, long usageCount,
                                   String createdBy, Instant createdAt, String modifiedBy, Instant modifiedAt) {}

    public record UserResponse(Long id, String code, String name, String email, String role, String department) {}

    public record LoginRequest(@NotNull(message = "userId is required") Long userId) {}

    public record LoginResponse(String token, Instant expiresAt, UserResponse user) {}

    public record Lookups(List<String> statuses, List<String> results, List<String> departments, List<String> responsiblePersons,
                          List<String> allowedExtensions, long maxFileSizeBytes) {}

    public record Summary(long total, java.util.Map<String, Long> byStatus) {}
}
