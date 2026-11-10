package cz.metdat.service;

import cz.metdat.domain.Attachment;
import cz.metdat.domain.TechnicalTest;
import cz.metdat.domain.TestStatus;
import cz.metdat.dto.Dtos.AttachmentResponse;
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
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

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

    public List<ValidatedUpload> validateAll(List<MultipartFile> files) {
        List<MultipartFile> real = files == null ? List.of() : files.stream().filter(f -> f != null && !(f.isEmpty() && (f.getOriginalFilename() == null || f.getOriginalFilename().isBlank()))).toList();
        return real.stream().map(validator::validate).toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long testId) {
        if (!tests.existsById(testId)) throw new NotFoundException("Technical test " + testId + " not found");
        return attachments.findByTechnicalTestIdOrderByUploadedAtAscIdAsc(testId).stream().map(mapper::toAttachment).toList();
    }

    @Transactional
    public List<AttachmentResponse> upload(Long testId, List<MultipartFile> files) {
        List<ValidatedUpload> valid = validateAll(files);
        if (valid.isEmpty()) throw new ValidationException("files", "No file was selected.");
        TechnicalTest test = tests.findByIdForUpdate(testId).orElseThrow(() -> new NotFoundException("Technical test " + testId + " not found"));
        List<Attachment> saved = store(test, valid);
        audit.modified(test);
        return saved.stream().map(mapper::toAttachment).toList();
    }

    /** Persists already validated uploads for a test (caller holds the transaction). Removes written files again if the tx rolls back. */
    public List<Attachment> store(TechnicalTest test, List<ValidatedUpload> uploads) {
        List<Attachment> saved = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) keys.forEach(storage::delete);
                }
            });
        }
        for (ValidatedUpload u : uploads) {
            String key = test.getId() + "/" + u.storedFilename();
            try { storage.store(key, u.bytes()); } catch (IOException e) { throw new IllegalStateException("Could not store attachment", e); }
            keys.add(key);
            Attachment a = new Attachment();
            a.setTechnicalTest(test);
            a.setOriginalFilename(u.originalFilename());
            a.setStoredFilename(u.storedFilename());
            a.setContentType(u.contentType());
            a.setFileSize(u.bytes().length);
            a.setStorageLocation(key);
            a.setUploadedBy(currentUser.email());
            a.setUploadedAt(Instant.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS));
            saved.add(attachments.save(a));
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Download download(Long testId, Long attachmentId) {
        Attachment a = find(testId, attachmentId);
        try { return new Download(a, storage.open(a.getStorageLocation())); }
        catch (IOException e) { throw new NotFoundException("The attachment file is no longer available in storage"); }
    }

    /** F06: serialised with status changes through the row lock on the test. */
    @Transactional
    public void delete(Long testId, Long attachmentId) {
        TechnicalTest test = tests.findByIdForUpdate(testId).orElseThrow(() -> new NotFoundException("Technical test " + testId + " not found"));
        Attachment a = find(testId, attachmentId);
        if (test.getStatus() == TestStatus.COMPLETED && attachments.countByTechnicalTestId(testId) <= 1) {
            throw new BusinessRuleException(LAST_ATTACHMENT_CODE,
                "The last attachment of a Completed test cannot be deleted. Change the status to Draft or In Progress first.");
        }
        String key = a.getStorageLocation();
        attachments.delete(a);
        audit.modified(test);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { storage.delete(key); }
        });
    }

    private Attachment find(Long testId, Long attachmentId) {
        Attachment a = attachments.findById(attachmentId).orElseThrow(() -> new NotFoundException("Attachment " + attachmentId + " not found"));
        if (!a.getTechnicalTest().getId().equals(testId)) throw new NotFoundException("Attachment " + attachmentId + " not found on test " + testId);
        return a;
    }
}
