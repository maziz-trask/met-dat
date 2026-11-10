package cz.metdat.repository;

import cz.metdat.domain.TechnicalTest;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;

public interface TechnicalTestRepository extends JpaRepository<TechnicalTest, Long>, JpaSpecificationExecutor<TechnicalTest> {

    @Override
    @EntityGraph(attributePaths = "testType")
    Page<TechnicalTest> findAll(Specification<TechnicalTest> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "testType")
    Optional<TechnicalTest> findById(Long id);

    /** Row lock: serialises status changes and attachment add/delete on the same test (F06 concurrency). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TechnicalTest t join fetch t.testType where t.id = :id")
    Optional<TechnicalTest> findByIdForUpdate(Long id);

    @Query(value = "select nextval('test_number_seq')", nativeQuery = true)
    long nextTestSequence();

    @Query("select t.status, count(t) from TechnicalTest t group by t.status")
    List<Object[]> countByStatus();

    @Query("select distinct t.department from TechnicalTest t order by t.department")
    List<String> distinctDepartments();
}
