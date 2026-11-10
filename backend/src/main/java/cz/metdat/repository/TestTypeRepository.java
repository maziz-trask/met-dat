package cz.metdat.repository;

import cz.metdat.domain.TestType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TestTypeRepository extends JpaRepository<TestType, Long> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<TestType> findAllByOrderByNameAsc();

    @Query("select t.testType.id, count(t) from TechnicalTest t group by t.testType.id")
    List<Object[]> usageCounts();
}
