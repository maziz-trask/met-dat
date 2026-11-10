package cz.metdat.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "attachment")
public class Attachment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technical_test_id", nullable = false, updatable = false)
    private TechnicalTest technicalTest;
    @Column(name = "original_filename", nullable = false) private String originalFilename;
    @Column(name = "stored_filename", nullable = false) private String storedFilename;
    @Column(name = "content_type", nullable = false) private String contentType;
    @Column(name = "file_size", nullable = false) private long fileSize;
    @Column(name = "storage_location", nullable = false) private String storageLocation;
    @Column(nullable = false) private boolean synthetic;
    @Column(name = "uploaded_by", nullable = false) private String uploadedBy;
    @Column(name = "uploaded_at", nullable = false) private Instant uploadedAt;

    public Long getId() { return id; }
    public TechnicalTest getTechnicalTest() { return technicalTest; }
    public void setTechnicalTest(TechnicalTest t) { this.technicalTest = t; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String v) { this.originalFilename = v; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String v) { this.storedFilename = v; }
    public String getContentType() { return contentType; }
    public void setContentType(String v) { this.contentType = v; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long v) { this.fileSize = v; }
    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String v) { this.storageLocation = v; }
    public boolean isSynthetic() { return synthetic; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String v) { this.uploadedBy = v; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant v) { this.uploadedAt = v; }
}
