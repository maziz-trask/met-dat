package cz.metdat.dto;

import java.time.Instant;

public record AttachmentResponse(Long id, String originalFilename, String contentType, long fileSize, boolean synthetic,
                                 String uploadedBy, Instant uploadedAt) {}
