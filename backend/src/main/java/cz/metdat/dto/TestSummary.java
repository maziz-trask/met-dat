package cz.metdat.dto;

import cz.metdat.domain.TestResult;
import cz.metdat.domain.TestStatus;
import java.time.LocalDate;

/** One row of the overview list. */
public record TestSummary(Long id, String testNumber, String vin, Long testTypeId, String testTypeName, boolean testTypeActive,
                          String department, LocalDate testDate, String testWeek, LocalDate periodStart, LocalDate periodEnd,
                          String responsiblePerson, TestStatus status, TestResult result, long attachmentCount) {}
