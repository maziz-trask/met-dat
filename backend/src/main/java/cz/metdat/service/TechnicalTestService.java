package cz.metdat.service;

import cz.metdat.domain.*;
import cz.metdat.dto.Dtos.*;
import cz.metdat.dto.TestFilter;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class TechnicalTestService {
    /** Public sort keys -> entity property paths (whitelist: unknown keys are rejected). */
    private static final Map<String, String> SORTABLE = Map.ofEntries(
        Map.entry("testNumber", "testNumber"), Map.entry("vin", "vin"), Map.entry("testType", "testType.name"),
        Map.entry("testDate", "periodStart"), Map.entry("responsiblePerson", "responsiblePerson"), Map.entry("status", "status"),
        Map.entry("result", "result"), Map.entry("department", "department"), Map.entry("modifiedAt", "modifiedAt"), Map.entry("createdAt", "createdAt"));

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

    public static Sort sortOf(String sort) {
        Sort.Order primary;
        if (!StringUtils.hasText(sort)) {
            primary = Sort.Order.desc("periodStart");
        } else {
            String[] p = sort.split(",");
            String prop = SORTABLE.get(p[0].trim());
            if (prop == null) throw new ValidationException("sort", "Unsupported sort field '" + p[0] + "'. Allowed: " + new TreeSet<>(SORTABLE.keySet()));
            primary = p.length > 1 && p[1].trim().equalsIgnoreCase("desc") ? Sort.Order.desc(prop) : Sort.Order.asc(prop);
        }
        return Sort.by(primary, Sort.Order.asc("id"));
    }

    @Transactional(readOnly = true)
    public Page<TestSummary> search(TestFilter filter, int page, int size, String sort) {
        Page<TechnicalTest> result = tests.findAll(TestSpecifications.of(filter), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), sortOf(sort)));
        Map<Long, Long> counts = counts(result.getContent());
        return result.map(t -> mapper.toSummary(t, counts.getOrDefault(t.getId(), 0L)));
    }

    /** All matches, in chunks, for the export (F09). */
    @Transactional(readOnly = true)
    public List<TechnicalTest> findAllMatching(TestFilter filter, String sort) {
        List<TechnicalTest> all = new ArrayList<>();
        Sort s = sortOf(sort);
        for (int page = 0;; page++) {
            Page<TechnicalTest> p = tests.findAll(TestSpecifications.of(filter), PageRequest.of(page, 500, s));
            all.addAll(p.getContent());
            if (!p.hasNext()) break;
        }
        return all;
    }

    @Transactional(readOnly = true)
    public Map<Long, List<String>> attachmentNames(Collection<Long> testIds) {
        if (testIds.isEmpty()) return Map.of();
        Map<Long, List<String>> m = new HashMap<>();
        for (Object[] r : attachments.namesByTestIds(testIds)) m.computeIfAbsent((Long) r[0], k -> new ArrayList<>()).add((String) r[1]);
        return m;
    }

    private Map<Long, Long> counts(List<TechnicalTest> list) {
        if (list.isEmpty()) return Map.of();
        return attachments.countsByTestIds(list.stream().map(TechnicalTest::getId).toList()).stream()
            .collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    @Transactional(readOnly = true)
    public TestDetail get(Long id) {
        TechnicalTest t = tests.findById(id).orElseThrow(() -> notFound(id));
        return mapper.toDetail(t, attachments.findByTechnicalTestIdOrderByUploadedAtAscIdAsc(id));
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        Map<String, Long> by = new LinkedHashMap<>();
        for (TestStatus s : TestStatus.values()) by.put(s.label(), 0L);
        long total = 0;
        for (Object[] r : tests.countByStatus()) { by.put(((TestStatus) r[0]).label(), (Long) r[1]); total += (Long) r[1]; }
        return new Summary(total, by);
    }

    @Transactional
    public TestDetail create(TestRequest req, List<MultipartFile> files) {
        List<ValidatedUpload> uploads = attachmentService.validateAll(files);
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
        if (req.version() != null && req.version() != t.getVersion()) {
            throw new BusinessRuleException("CONCURRENT_MODIFICATION", "The record was changed by someone else. Reload it and try again.");
        }
        TestType type = t.getTestType();
        // an already assigned (possibly inactive) type may stay; any different type must be active
        if (!Objects.equals(type.getId(), req.testTypeId())) type = activeType(req.testTypeId());
        long count = attachments.countByTechnicalTestId(id);
        boolean completionRelevant = req.status() == TestStatus.COMPLETED && (t.getStatus() != TestStatus.COMPLETED || req.result() != t.getResult());
        if (completionRelevant) CompletionRule.check(req.result(), count);
        apply(t, req, type);
        audit.modified(t);
        return mapper.toDetail(t, attachments.findByTechnicalTestIdOrderByUploadedAtAscIdAsc(id));
    }

    /** F06: every transition into Completed is validated here (and in update/create), inside the row lock. */
    @Transactional
    public TestDetail changeStatus(Long id, TestStatus target) {
        TechnicalTest t = tests.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
        if (target != t.getStatus()) {
            if (target == TestStatus.COMPLETED) CompletionRule.check(t.getResult(), attachments.countByTechnicalTestId(id));
            t.setStatus(target);
            audit.modified(t);
        }
        return mapper.toDetail(t, attachments.findByTechnicalTestIdOrderByUploadedAtAscIdAsc(id));
    }

    private TestType activeType(Long id) {
        TestType type = types.findById(id).orElseThrow(() -> new ValidationException("testTypeId", "Test type " + id + " does not exist"));
        if (!type.isActive()) throw new ValidationException("testTypeId", "Test type '" + type.getName() + "' is inactive and cannot be newly assigned");
        return type;
    }

    private void apply(TechnicalTest t, TestRequest r, TestType type) {
        IsoWeeks.Period period;
        if (r.testDate() != null) period = IsoWeeks.ofDate(r.testDate());
        else if (StringUtils.hasText(r.testWeek())) period = IsoWeeks.ofLabel(r.testWeek());
        else throw new ValidationException("testDate", "Test date (or calendar week) is required");
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

    private static NotFoundException notFound(Long id) { return new NotFoundException("Technical test " + id + " not found"); }
}
