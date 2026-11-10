package cz.metdat.web;

import cz.metdat.domain.TestStatus;
import cz.metdat.dto.*;
import cz.metdat.exception.ValidationException;
import cz.metdat.service.ExportService;
import cz.metdat.service.LookupService;
import cz.metdat.service.TechnicalTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Tag(name = "Technical tests")
public class TechnicalTestController {
    private final TechnicalTestService service;
    private final ExportService export;
    private final LookupService lookups;

    public TechnicalTestController(TechnicalTestService service, ExportService export, LookupService lookups) {
        this.service = service; this.export = export; this.lookups = lookups;
    }

    private static TestFilter filter(String vin, Long testTypeId, String status, LocalDate from, LocalDate to) {
        TestStatus parsed = status == null || status.isBlank() ? null : TestStatus.fromLabel(status);
        return new TestFilter(vin, testTypeId, parsed, from, to);
    }

    @GetMapping("/tests")
    @Operation(summary = "List/search tests (server-side filtering, sorting, pagination). Date range matches the ISO-week period (Mon-Sun) overlap.")
    public Page<TestSummary> list(@RequestParam(required = false) String vin, @RequestParam(required = false) Long testTypeId,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) String sort) {
        return service.search(filter(vin, testTypeId, status, dateFrom, dateTo), page, size, sort);
    }

    @GetMapping("/tests/export")
    @Operation(summary = "CSV export of ALL tests matching the filters (not limited to a page)")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(defaultValue = "csv") String format,
                                            @RequestParam(required = false) String vin, @RequestParam(required = false) Long testTypeId,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                                            @RequestParam(required = false) String sort) {
        if (!"csv".equalsIgnoreCase(format)) throw new ValidationException("format", "export.format");
        byte[] body = export.csv(filter(vin, testTypeId, status, dateFrom, dateTo), sort);
        return ResponseEntity.ok()
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("technical-tests-" + LocalDate.now() + ".csv").build().toString())
            .body(body);
    }

    @GetMapping("/tests/summary")
    public Summary summary() { return service.summary(); }

    @GetMapping("/tests/{id}")
    public TestDetail get(@PathVariable Long id) { return service.get(id); }

    @PostMapping(value = "/tests", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EDITOR')")
    @Operation(summary = "Create a test. Multipart: part 'data' (JSON) + optional parts 'files'")
    public ResponseEntity<TestDetail> create(@RequestPart("data") @Valid TestRequest data,
                                             @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(data, files));
    }

    @PutMapping("/tests/{id}")
    @PreAuthorize("hasRole('EDITOR')")
    public TestDetail update(@PathVariable Long id, @Valid @RequestBody TestRequest req) { return service.update(id, req); }

    @PatchMapping("/tests/{id}/status")
    @PreAuthorize("hasRole('EDITOR')")
    @Operation(summary = "Change status. Completed requires result Passed/Failed and >= 1 attachment (409 otherwise)")
    public TestDetail changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest req) {
        return service.changeStatus(id, req.status());
    }

    @GetMapping("/lookups")
    @Tag(name = "Lookups")
    public Lookups lookups() { return lookups.lookups(); }
}
