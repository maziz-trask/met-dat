package cz.metdat.service;

import cz.metdat.domain.Attachment;
import cz.metdat.domain.TechnicalTest;
import cz.metdat.domain.TestStatus;
import cz.metdat.dto.AttachmentResponse;
import cz.metdat.exception.BusinessRuleException;
import cz.metdat.exception.NotFoundException;
import cz.metdat.exception.ValidationException;
import cz.metdat.mapper.TestMapper;
import cz.metdat.repository.AttachmentRepository;
import cz.metdat.repository.TechnicalTestRepository;
import cz.metdat.security.CurrentUser;
import cz.metdat.storage.AttachmentValidator;
import cz.metdat.storage.AttachmentValidator.ValidatedUpload;
import cz.metdat.storage.FileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** F07 + the attachment half of F06. Every mutation holds the row lock of the owning test. */
@Service
public class AttachmentService {
    public static final String LAST_ATTACHMENT_CODE = "LAST_ATTACHMENT_OF_COMPLETED_TEST";

    private final AttachmentRepository attachments;
    private final TechnicalTestRepository tests;
    private final FileStorage storage;
    private final AttachmentValidator validator;
    private final AuditStamper audit;
    private final CurrentUser currentUser;
    private final TestMapper mapper;
    private final Clock clock;

    public AttachmentService(AttachmentRepository attachments, TechnicalTestRepository tests, FileStorage storage,
                             AttachmentValidator validator, AuditStamper audit, CurrentUser currentUser, TestMapper mapper, Clock clock) {
        this.attachments = attachments; this.tests = tests; this.storage = storage; this.validator = validator;
        this.audit = audit; this.currentUser = currentUser; this.mapper = mapper; this.clock = clock;
    }

    public record Download(Attachment attachment, InputStream content) {}

    /** Validates all uploaded parts before anything is written; empty file inputs (no file chosen) are ignored. */
    public List<ValidatedUpload> validateAll(List<MultipartFile> files) {
        if (files == null) return List.of();
        return files.stream().filter(f -> f != null && !isNoFileSelected(f)).map(validator::validate).toList();
    }

    private static boolean isNoFileSelected(MultipartFile f) {
        return f.isEmpty() && (f.getOriginalFilename() == null || f.getOriginalFilename().isBlank());
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long testId) {
        if (!tests.existsById(testId)) throw testNotFound(testId);
        return attachments.findByTechnicalTestIdOrderByUploadedAtAscIdAsc(testId).stream().map(mapper::toAttachment).toList();
    }

    @Transactional
    public List<AttachmentResponse> upload(Long testId, List<MultipartFile> files) {
        List<ValidatedUpload> valid = validateAll(files);
        if (valid.isEmpty()) throw new ValidationException("files", "attachment.noFile");
        TechnicalTest test = tests.findByIdForUpdate(testId).orElseThrow(() -> testNotFound(testId));
        List<Attachment> saved = store(test, valid);
        audit.modified(test);
        return saved.stream().map(mapper::toAttachment).toList();
    }

    /** Persists already validated uploads (caller holds the transaction). Files written by a transaction that rolls back are removed again. */
    public List<Attachment> store(TechnicalTest test, List<ValidatedUpload> uploads) {
        List<String> writtenKeys = new ArrayList<>();
        removeFilesIfRolledBack(writtenKeys);
        List<Attachment> saved = new ArrayList<>();
        for (ValidatedUpload upload : uploads) {
            String key = test.getId() + "/" + upload.storedFilename();
            try { storage.store(key, upload.bytes()); } catch (IOException e) { throw new IllegalStateException("Could not store attachment", e); }
            writtenKeys.add(key);
            saved.add(attachments.save(toEntity(test, upload, key)));
        }
        return saved;
    }

    private Attachment toEntity(TechnicalTest test, ValidatedUpload upload, String key) {
        Attachment a = new Attachment();
        a.setTechnicalTest(test);
        a.setOriginalFilename(upload.originalFilename());
        a.setStoredFilename(upload.storedFilename());
        a.setContentType(upload.contentType());
        a.setFileSize(upload.bytes().length);
        a.setStorageLocation(key);
        a.setUploadedBy(currentUser.email());
        a.setUploadedAt(Instant.now(clock).truncatedTo(ChronoUnit.MICROS));
        return a;
    }

    private void removeFilesIfRolledBack(List<String> keys) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) keys.forEach(storage::delete);
            }
        });
    }

    @Transactional(readOnly = true)
    public Download download(Long testId, Long attachmentId) {
        Attachment a = find(testId, attachmentId);
        try { return new Download(a, storage.open(a.getStorageLocation())); }
        catch (IOException e) { throw new NotFoundException("attachment.contentGone"); }
    }

    /** F06: serialised with status changes through the row lock on the test. */
    @Transactional
    public void delete(Long testId, Long attachmentId) {
        TechnicalTest test = tests.findByIdForUpdate(testId).orElseThrow(() -> testNotFound(testId));
        Attachment a = find(testId, attachmentId);
        if (test.getStatus() == TestStatus.COMPLETED && attachments.countByTechnicalTestId(testId) <= 1) {
            throw new BusinessRuleException(LAST_ATTACHMENT_CODE, "attachment.lastOfCompleted");
        }
        String key = a.getStorageLocation();
        attachments.delete(a);
        audit.modified(test);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { storage.delete(key); }
        });
    }

    private Attachment find(Long testId, Long attachmentId) {
        Attachment a = attachments.findById(attachmentId).orElseThrow(() -> new NotFoundException("attachment.notFound", String.valueOf(attachmentId)));
        if (!a.getTechnicalTest().getId().equals(testId)) {
            throw new NotFoundException("attachment.notOnTest", String.valueOf(attachmentId), String.valueOf(testId));
        }
        return a;
    }

    private static NotFoundException testNotFound(Long id) { return new NotFoundException("test.notFound", String.valueOf(id)); }
}
