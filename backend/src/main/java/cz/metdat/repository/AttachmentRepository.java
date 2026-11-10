package cz.metdat.repository;

import cz.metdat.domain.Attachment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByTechnicalTestIdOrderByUploadedAtAscIdAsc(Long testId);
    long countByTechnicalTestId(Long testId);
    List<Attachment> findBySyntheticTrue();

    @Query("select a.technicalTest.id, count(a) from Attachment a where a.technicalTest.id in :ids group by a.technicalTest.id")
    List<Object[]> countsByTestIds(Collection<Long> ids);

    @Query("select a.technicalTest.id, a.originalFilename from Attachment a where a.technicalTest.id in :ids order by a.id")
    List<Object[]> namesByTestIds(Collection<Long> ids);
}
