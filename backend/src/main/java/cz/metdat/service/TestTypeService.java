package cz.metdat.service;

import cz.metdat.domain.TestType;
import cz.metdat.dto.TestTypeRequest;
import cz.metdat.dto.TestTypeResponse;
import cz.metdat.exception.NotFoundException;
import cz.metdat.exception.ValidationException;
import cz.metdat.mapper.TestMapper;
import cz.metdat.repository.TestTypeRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** F10. Types are never deleted: deactivation only flips the flag, historical tests keep their reference. */
@Service
public class TestTypeService {
    private final TestTypeRepository types;
    private final AuditStamper audit;
    private final TestMapper mapper;

    public TestTypeService(TestTypeRepository types, AuditStamper audit, TestMapper mapper) {
        this.types = types; this.audit = audit; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TestTypeResponse> list(boolean activeOnly) {
        Map<Long, Long> usage = usageCounts();
        return types.findAllByOrderByNameAsc().stream().filter(t -> !activeOnly || t.isActive())
            .map(t -> mapper.toType(t, usage.getOrDefault(t.getId(), 0L))).toList();
    }

    @Transactional
    public TestTypeResponse create(TestTypeRequest req) {
        String name = req.name().trim();
        if (types.existsByNameIgnoreCase(name)) throw new ValidationException("name", "testType.duplicate", name);
        TestType t = new TestType();
        t.setName(name);
        t.setDescription(blankToNull(req.description()));
        t.setActive(req.active() == null || req.active());
        audit.created(t);
        return mapper.toType(types.save(t), 0);
    }

    @Transactional
    public TestTypeResponse update(Long id, TestTypeRequest req) {
        TestType t = find(id);
        String name = req.name().trim();
        if (types.existsByNameIgnoreCaseAndIdNot(name, id)) throw new ValidationException("name", "testType.duplicate", name);
        t.setName(name);
        t.setDescription(blankToNull(req.description()));
        if (req.active() != null) t.setActive(req.active());
        audit.modified(t);
        return withUsage(t);
    }

    @Transactional
    public TestTypeResponse setActive(Long id, boolean active) {
        TestType t = find(id);
        if (t.isActive() != active) {
            t.setActive(active);
            audit.modified(t);
        }
        return withUsage(t);
    }

    private Map<Long, Long> usageCounts() {
        return types.usageCounts().stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));
    }

    private TestTypeResponse withUsage(TestType t) { return mapper.toType(t, usageCounts().getOrDefault(t.getId(), 0L)); }

    private TestType find(Long id) { return types.findById(id).orElseThrow(() -> new NotFoundException("testType.notFound", String.valueOf(id))); }

    private static String blankToNull(String s) { return s == null || s.isBlank() ? null : s.trim(); }
}
