package cz.metdat.web;

import cz.metdat.dto.Dtos.AttachmentResponse;
import cz.metdat.service.AttachmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tests/{testId}/attachments")
@Tag(name = "Attachments")
public class AttachmentController {
    private final AttachmentService service;

    public AttachmentController(AttachmentService service) { this.service = service; }

    @GetMapping
    public List<AttachmentResponse> list(@PathVariable Long testId) { return service.list(testId); }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<List<AttachmentResponse>> upload(@PathVariable Long testId, @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(testId, files));
    }

    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long testId, @PathVariable Long attachmentId) {
        AttachmentService.Download d = service.download(testId, attachmentId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(d.attachment().getContentType()))
            .contentLength(d.attachment().getFileSize())
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(d.attachment().getOriginalFilename(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
            .body(new InputStreamResource(d.content()));
    }

    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasRole('EDITOR')")
    public ResponseEntity<Void> delete(@PathVariable Long testId, @PathVariable Long attachmentId) {
        service.delete(testId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
