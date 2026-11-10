package cz.metdat.dto;

import cz.metdat.domain.TestResult;
import cz.metdat.domain.TestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Everything about one test including attachments and audit data. */
public record TestDetail(Long id, String testNumber, String vin, Long testTypeId, String testTypeName, boolean testTypeActive,
                         String department, LocalDate testDate, String testWeek, LocalDate periodStart, LocalDate periodEnd,
                         String responsiblePerson, TestStatus status, TestResult result, String comment,
                         List<AttachmentResponse> attachments, String createdBy, Instant createdAt,
                         String modifiedBy, Instant modifiedAt, long version) {}
