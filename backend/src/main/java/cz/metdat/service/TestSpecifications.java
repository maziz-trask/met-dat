package cz.metdat.service;

import cz.metdat.domain.TechnicalTest;
import cz.metdat.dto.TestFilter;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** Translates a TestFilter to SQL (F05). Date range = overlap of [dateFrom, dateTo] with the test's Monday-Sunday period. */
public final class TestSpecifications {
    private TestSpecifications() {}

    public static Specification<TechnicalTest> of(TestFilter f) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (StringUtils.hasText(f.vin())) {
                String like = f.vin().trim().toUpperCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                p.add(cb.like(cb.upper(root.get("vin")), "%" + like + "%", '\\'));
            }
            if (f.testTypeId() != null) p.add(cb.equal(root.get("testType").get("id"), f.testTypeId()));
            if (f.status() != null) p.add(cb.equal(root.get("status"), f.status()));
            if (f.dateFrom() != null) p.add(cb.greaterThanOrEqualTo(root.get("periodEnd"), f.dateFrom()));
            if (f.dateTo() != null) p.add(cb.lessThanOrEqualTo(root.get("periodStart"), f.dateTo()));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
