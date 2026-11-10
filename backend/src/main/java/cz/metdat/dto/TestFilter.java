package cz.metdat.dto;

import cz.metdat.domain.TestStatus;
import java.time.LocalDate;

/** Search criteria shared by the list endpoint and the CSV export (F05, F09). */
public record TestFilter(String vin, Long testTypeId, TestStatus status, LocalDate dateFrom, LocalDate dateTo) {}
