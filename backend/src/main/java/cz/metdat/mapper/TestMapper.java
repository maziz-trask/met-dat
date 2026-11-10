package cz.metdat.mapper;

import cz.metdat.domain.*;
import cz.metdat.dto.Dtos.*;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TestMapper {
    public TestSummary toSummary(TechnicalTest t, long attachmentCount) {
        TestType type = t.getTestType();
        return new TestSummary(t.getId(), t.getTestNumber(), t.getVin(), type.getId(), type.getName(), type.isActive(),
            t.getDepartment(), t.getTestDate(), t.getWeekLabel(), t.getPeriodStart(), t.getPeriodEnd(),
            t.getResponsiblePerson(), t.getStatus(), t.getResult(), attachmentCount);
    }

    public TestDetail toDetail(TechnicalTest t, List<Attachment> attachments) {
        TestType type = t.getTestType();
        return new TestDetail(t.getId(), t.getTestNumber(), t.getVin(), type.getId(), type.getName(), type.isActive(),
            t.getDepartment(), t.getTestDate(), t.getWeekLabel(), t.getPeriodStart(), t.getPeriodEnd(),
            t.getResponsiblePerson(), t.getStatus(), t.getResult(), t.getComment(),
            attachments.stream().map(this::toAttachment).toList(),
            t.getCreatedBy(), t.getCreatedAt(), t.getModifiedBy(), t.getModifiedAt(), t.getVersion());
    }

    public AttachmentResponse toAttachment(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getOriginalFilename(), a.getContentType(), a.getFileSize(), a.isSynthetic(),
            a.getUploadedBy(), a.getUploadedAt());
    }

    public TestTypeResponse toType(TestType t, long usage) {
        return new TestTypeResponse(t.getId(), t.getCode(), t.getName(), t.getDescription(), t.isActive(), usage,
            t.getCreatedBy(), t.getCreatedAt(), t.getModifiedBy(), t.getModifiedAt());
    }

    public UserResponse toUser(AppUser u) {
        return new UserResponse(u.getId(), u.getCode(), u.getName(), u.getEmail(), u.getRole().name(), u.getDepartment());
    }
}
