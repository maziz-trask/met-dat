package cz.metdat.service;

import cz.metdat.domain.*;
import cz.metdat.dto.*;
import cz.metdat.exception.BusinessRuleException;
import cz.metdat.exception.NotFoundException;
import cz.metdat.exception.ValidationException;
import cz.metdat.mapper.TestMapper;
import cz.metdat.repository.AttachmentRepository;
import cz.metdat.repository.TechnicalTestRepository;
import cz.metdat.repository.TestTypeRepository;
import cz.metdat.storage.AttachmentValidator.ValidatedUpload;
import java.time.Clock;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/** F01-F06: search, create, edit, detail and the status workflow of technical tests. */
@Service
public class TechnicalTestService {
    /** Public sort keys -> entity property paths (whitelist: unknown keys are rejected). */
    private static final Map<String, String> SORTABLE = Map.ofEntries(
        Map.entry("testNumber", "testNumber"), Map.entry("vin", "vin"), Map.entry("testType", "testType.name"),
        Map.entry("testDate", "periodStart"), Map.entry("responsiblePerson", "responsiblePerson"), Map.entry("status", "status"),
        Map.entry("result", "result"), Map.entry("department", "department"), Map.entry("modifiedAt", "modifiedAt"), Map.entry("createdAt", "createdAt"));
    private static final int MAX_PAGE_SIZE = 100;
    private static final int EXPORT_CHUNK = 500;

    private final TechnicalTestRepository tests;
    private final TestTypeRepository types;
    private final AttachmentRepository attachments;
    private final AttachmentService attachmentService;
    private final AuditStamper audit;
    private final TestMapper mapper;
    private final Clock clock;

    public TechnicalTestService(TechnicalTestRepository tests, TestTypeRepository types, AttachmentRepository attachments,
                                AttachmentService attachmentService, AuditStamper audit, TestMapper mapper, Clock clock) {
        this.tests = tests; this.types = types; this.attachments = attachments; this.attachmentService = attachmentService;
        this.audit = audit; this.mapper = mapper; this.clock = clock;
    }

    // ---------------------------------------------------------------- queries

    public static Sort sortOf(String sort) {
        Sort.Order primary;
        if (!StringUtils.hasText(sort)) {
            primary = Sort.Order.desc("periodStart");
        } else {
            String[] parts = sort.split(",");
            String property = SORTABLE.get(parts[0].trim());
            if (property == null) throw new ValidationException("sort", "sort.unsupported", parts[0], String.join(", ", new TreeSet<>(SORTABLE.keySet())));
            boolean descending = parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc");
            primary = descending ? Sort.Order.desc(property) : Sort.Order.asc(property);
        }
        return Sort.by(primary, Sort.Order.asc("id"));   // id as tie-breaker keeps pagination stable
    }

    @Transactional(readOnly = true)
    public Page<TestSummary> search(TestFilter filter, int page, int size, String sort) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sortOf(sort));
        Page<TechnicalTest> result = tests.findAll(TestSpecifications.of(filter), pageable);
        Map<Long, Long> counts = attachmentCounts(result.getContent());
        return result.map(t -> mapper.toSummary(t, counts.getOrDefault(t.getId(), 0L)));
    }

    /** All matches, read in chunks, for the export (F09). */
    @Transactional(readOnly = true)
    public List<TechnicalTest> findAllMatching(TestFilter filter, String sort) {
        List<TechnicalTest> all = new ArrayList<>();
        Sort order = sortOf(sort);
        for (int page = 0;; page++) {
            Page<TechnicalTest> chunk = tests.findAll(TestSpecifications.of(filter), PageRequest.of(page, EXPORT_CHUNK, order));
            all.addAll(chunk.getContent());
            if (!chunk.hasNext()) break;
        }
        return all;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> attachmentNames(Collection<Long> testIds) {
        if (testIds.isEmpty()) return Map.of();
        Map<Long, List<String>> names = new HashMap<>();
        for (Object[] row : attachments.namesByTestIds(testIds)) names.computeIfAbsent((Long) row[0], k -> new ArrayList<>()).add((String) row[1]);
        return names;
    }

    private Map<Long, Long> attachmentCounts(List<TechnicalTest> list) {
        if (list.isEmpty()) return Map.of();
        return attachments.countsByTestIds(list.stream().map(TechnicalTest::getId).toList()).stream()
            .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    @Transactional(readOnly = true)
    public TestDetail get(Long id) {
        TechnicalTest t = tests.findById(id).orElseThrow(() -> notFound(id));
        return detail(t);
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TestStatus s : TestStatus.values()) byStatus.put(s.label(), 0L);
        long total = 0;
        for (Object[] row : tests.countByStatus()) {
            byStatus.put(((TestStatus) row[0]).label(), (Long) row[1]);
            total += (Long) row[1];
        }
        return new Summary(total, byStatus);
    }

    // --------------------------------------------------------------- commands

    @Transactional
    public TestDetail create(TestRequest req, List<MultipartFile> files) {
        List<ValidatedUpload> uploads = attachmentService.validateAll(files);   // fail before anything is written
        TestType type = activeType(req.testTypeId());
        TechnicalTest t = new TechnicalTest();
        apply(t, req, type);
        if (req.status() == TestStatus.COMPLETED) CompletionRule.check(req.result(), uploads.size());
        t.setTestNumber("TEST-%d-%04d".formatted(Year.now(clock).getValue(), tests.nextTestSequence()));
        audit.created(t);
        tests.save(t);
        List<Attachment> saved = attachmentService.store(t, uploads);
        return mapper.toDetail(t, saved);
    }

    @Transactional
    public TestDetail update(Long id, TestRequest req) {
        TechnicalTest t = tests.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (req.version() != null && req.version() != t.getVersion()) throw new BusinessRuleException("CONCURRENT_MODIFICATION", "error.concurrent");
        TestType type = t.getTestType();
        // an already assigned (possibly inactive) type may stay; any different type must be active
        if (!Objects.equals(type.getId(), req.testTypeId())) type = activeType(req.testTypeId());
        // F06 is re-checked when entering Completed or changing the result of a Completed test; other edits of legacy rows stay possible
        boolean completionRelevant = req.status() == TestStatus.COMPLETED && (t.getStatus() != TestStatus.COMPLETED || req.result() != t.getResult());
        if (completionRelevant) CompletionRule.check(req.result(), attachments.countByTechnicalTestId(id));
        apply(t, req, type);
        audit.modified(t);
        return detail(t);
    }

    /** F06: every transition into Completed is validated here (and in create/update), inside the row lock. */
    @Transactional
    public TestDetail changeStatus(Long id, TestStatus target) {
        TechnicalTest t = tests.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (target != t.getStatus()) {
            if (target == TestStatus.COMPLETED) CompletionRule.check(t.getResult(), attachments.countByTechnicalTestId(id));
            t.setStatus(target);
            audit.modified(t);
        }
        return detail(t);
    }

    // ---------------------------------------------------------------- helpers

    private TestDetail detail(TechnicalTest t) {
        return mapper.toDetail(t, attachments.findByTechnicalTestIdOrderByUploadedAtAscIdAsc(t.getId()));
    }

    private TestType activeType(Long id) {
        TestType type = types.findById(id).orElseThrow(() -> new ValidationException("testTypeId", "test.type.notFound", String.valueOf(id)));
        if (!type.isActive()) throw new ValidationException("testTypeId", "test.type.inactive", type.getName());
        return type;
    }

    private void apply(TechnicalTest t, TestRequest r, TestType type) {
        IsoWeeks.Period period = periodOf(r);
        t.setVin(r.vin().trim().toUpperCase());
        t.setTestType(type);
        t.setDepartment(r.department().trim());
        t.setTestYear(period.year());
        t.setTestWeek(period.week());
        t.setTestDate(period.exactDate());
        t.setPeriodStart(period.start());
        t.setPeriodEnd(period.end());
        t.setResponsiblePerson(r.responsiblePerson().trim());
        t.setStatus(r.status());
        t.setResult(r.result());
        t.setComment(StringUtils.hasText(r.comment()) ? r.comment().trim() : null);
    }

    private static IsoWeeks.Period periodOf(TestRequest r) {
        if (r.testDate() != null) return IsoWeeks.ofDate(r.testDate());
        if (StringUtils.hasText(r.testWeek())) return IsoWeeks.ofLabel(r.testWeek());
        throw new ValidationException("testDate", "test.dateRequired");
    }

    private static NotFoundException notFound(Long id) { return new NotFoundException("test.notFound", String.valueOf(id)); }
}
