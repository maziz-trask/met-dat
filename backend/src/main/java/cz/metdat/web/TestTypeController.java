package cz.metdat.web;

import cz.metdat.dto.Dtos.*;
import cz.metdat.service.TestTypeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test-types")
@Tag(name = "Test types")
public class TestTypeController {
    private final TestTypeService service;

    public TestTypeController(TestTypeService service) { this.service = service; }

    @GetMapping
    public List<TestTypeResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) { return service.list(activeOnly); }

    @PostMapping
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<TestTypeResponse> create(@Valid @RequestBody TestTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EDITOR')")
    public TestTypeResponse update(@PathVariable Long id, @Valid @RequestBody TestTypeRequest req) { return service.update(id, req); }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('EDITOR')")
    public TestTypeResponse activate(@PathVariable Long id) { return service.setActive(id, true); }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('EDITOR')")
    public TestTypeResponse deactivate(@PathVariable Long id) { return service.setActive(id, false); }
}
