package cz.metdat.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import cz.metdat.config.AppProperties;
import cz.metdat.domain.*;
import cz.metdat.exception.BusinessRuleException;
import cz.metdat.exception.ValidationException;
import cz.metdat.mapper.TestMapper;
import cz.metdat.repository.*;
import cz.metdat.service.*;
import cz.metdat.storage.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class UnitTests {

    // ---- F06 rule ----
    @Test void completionRuleAcceptsPassedAndFailedWithAttachment() {
        CompletionRule.check(TestResult.PASSED, 1);
        CompletionRule.check(TestResult.FAILED, 3);
    }

    @Test void completionRuleRejectsEverythingElse() {
        for (TestResult r : TestResult.values()) {
            if (r == TestResult.PASSED || r == TestResult.FAILED) continue;
            assertThatThrownBy(() -> CompletionRule.check(r, 2)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("Passed or Failed");
        }
        assertThatThrownBy(() -> CompletionRule.check(null, 1)).hasMessageContaining("empty");
        assertThatThrownBy(() -> CompletionRule.check(TestResult.PASSED, 0)).hasMessageContaining("at least one attachment");
        assertThatThrownBy(() -> CompletionRule.check(TestResult.NOT_EVALUATED, 0)).hasMessageContaining("Passed or Failed").hasMessageContaining("attachment");
    }

    // ---- service with mocks ----
    @Test void changeStatusToCompletedIsRejectedWithoutAttachmentAndNotSaved() {
        var tests = mock(TechnicalTestRepository.class);
        var attachments = mock(AttachmentRepository.class);
        var audit = mock(AuditStamper.class);
        var svc = new TechnicalTestService(tests, mock(TestTypeRepository.class), attachments, mock(AttachmentService.class), audit, new TestMapper(), Clock.systemUTC());
        TechnicalTest t = new TechnicalTest();
        t.setStatus(TestStatus.IN_PROGRESS);
        t.setResult(TestResult.PASSED);
        when(tests.findByIdForUpdate(5L)).thenReturn(Optional.of(t));
        when(attachments.countByTechnicalTestId(5L)).thenReturn(0L);
        assertThatThrownBy(() -> svc.changeStatus(5L, TestStatus.COMPLETED)).isInstanceOf(BusinessRuleException.class);
        assertThat(t.getStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        verify(audit, never()).modified(any(TechnicalTest.class));
    }

    @Test void reopeningNeverChecksTheRule() {
        var tests = mock(TechnicalTestRepository.class);
        var attachments = mock(AttachmentRepository.class);
        var type = new TestType();
        var svc = new TechnicalTestService(tests, mock(TestTypeRepository.class), attachments, mock(AttachmentService.class), mock(AuditStamper.class), new TestMapper(), Clock.systemUTC());
        TechnicalTest t = new TechnicalTest();
        t.setTestType(type);
        t.setStatus(TestStatus.COMPLETED);
        t.setResult(TestResult.CONDITIONALLY_PASSED);   // legacy row
        when(tests.findByIdForUpdate(5L)).thenReturn(Optional.of(t));
        when(attachments.findByTechnicalTestIdOrderByUploadedAtAscIdAsc(anyLong())).thenReturn(List.of());
        svc.changeStatus(5L, TestStatus.DRAFT);
        assertThat(t.getStatus()).isEqualTo(TestStatus.DRAFT);
    }

    // ---- ISO weeks ----
    @Test void isoWeekBoundaries() {
        var p = IsoWeeks.ofLabel("KT08/26");
        assertThat(p.start()).isEqualTo(LocalDate.of(2026, 2, 16));
        assertThat(p.end()).isEqualTo(LocalDate.of(2026, 2, 22));
        assertThat(IsoWeeks.ofLabel("KT01/26").start()).isEqualTo(LocalDate.of(2025, 12, 29));   // week 1 starts in previous year
        assertThat(IsoWeeks.ofLabel("KT53/26").end()).isEqualTo(LocalDate.of(2027, 1, 3));
        assertThatThrownBy(() -> IsoWeeks.ofLabel("KT54/26")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> IsoWeeks.ofLabel("KT53/27")).isInstanceOf(ValidationException.class);   // 2027 has 52 weeks
        assertThatThrownBy(() -> IsoWeeks.ofLabel("08/26")).isInstanceOf(ValidationException.class);
        var d = IsoWeeks.ofDate(LocalDate.of(2026, 3, 11));
        assertThat(d.week()).isEqualTo(11);
        assertThat(d.start()).isEqualTo(LocalDate.of(2026, 3, 11));
    }

    // ---- CSV export ----
    @Test void csvCellEscapingAndFormulaGuard() {
        var m = ExportServiceAccess.cell("a,b");
        assertThat(m).isEqualTo("\"a,b\"");
        assertThat(ExportServiceAccess.cell("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(ExportServiceAccess.cell("=SUM(A1)")).isEqualTo("'=SUM(A1)");
        assertThat(ExportServiceAccess.cell(null)).isEmpty();
    }

    // ---- attachments ----
    private AttachmentValidator validator() {
        return new AttachmentValidator(new AppProperties(null, null, null, null,
            new AppProperties.Upload(DataSize.ofBytes(1024), List.of("pdf", "csv"))));
    }

    @Test void validatorAcceptsPdfAndCsvAndGeneratesSafeStoredName() {
        var ok = validator().validate(new MockMultipartFile("files", "../../etc/Report 1.pdf", "application/pdf", "%PDF-1.4 x".getBytes()));
        assertThat(ok.originalFilename()).isEqualTo("Report 1.pdf");
        assertThat(ok.storedFilename()).matches("[0-9a-f-]{36}\\.pdf");
        assertThat(validator().validate(new MockMultipartFile("files", "m.csv", "application/vnd.ms-excel", "a,b".getBytes())).contentType()).isEqualTo("text/csv");
    }

    @Test void validatorRejectsBadFiles() {
        var v = validator();
        assertThatThrownBy(() -> v.validate(new MockMultipartFile("files", "x.exe", "application/pdf", "%PDF-".getBytes()))).hasMessageContaining("unsupported type");
        assertThatThrownBy(() -> v.validate(new MockMultipartFile("files", "x.docx", "application/pdf", "PK".getBytes()))).hasMessageContaining("unsupported type");   // not enabled in this config
        assertThatThrownBy(() -> v.validate(new MockMultipartFile("files", "x.pdf", "application/pdf", new byte[2000]))).hasMessageContaining("too large");
        assertThatThrownBy(() -> v.validate(new MockMultipartFile("files", "x.pdf", "text/html", "%PDF-".getBytes()))).hasMessageContaining("does not match");
        assertThatThrownBy(() -> v.validate(new MockMultipartFile("files", "x.pdf", "application/pdf", "<html>".getBytes()))).hasMessageContaining("does not look like");
        assertThatThrownBy(() -> v.validate(new MockMultipartFile("files", "..", "application/pdf", "%PDF-".getBytes()))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> v.validate(new MockMultipartFile("files", "noext", "application/pdf", "%PDF-".getBytes()))).isInstanceOf(ValidationException.class);
    }

    @Test void syntheticFilesPassOwnValidation() {
        byte[] pdf = SyntheticFiles.forFile("TEST-2026-0001_report.pdf", "TEST-2026-0001");
        byte[] csv = SyntheticFiles.forFile("TEST-2026-0004_measurement.csv", "TEST-2026-0004");
        assertThat(new String(pdf, StandardCharsets.ISO_8859_1)).startsWith("%PDF-1.4").endsWith("%%EOF\n").contains("SYNTHETIC DEMO FILE");
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("SYNTHETIC DEMO FILE");
        var v = new AttachmentValidator(new AppProperties(null, null, null, null, new AppProperties.Upload(DataSize.ofMegabytes(1), List.of("pdf", "csv"))));
        v.validate(new MockMultipartFile("files", "a.pdf", "application/pdf", pdf));
        v.validate(new MockMultipartFile("files", "a.csv", "text/csv", csv));
    }

    @Test void fileSystemStorageBlocksPathTraversal() throws Exception {
        var dir = java.nio.file.Files.createTempDirectory("metdat");
        var s = new FileSystemStorage(new AppProperties(null, null, null, new AppProperties.Storage("filesystem", dir.toString()), null));
        s.store("1/a.bin", new byte[] {1});
        assertThat(s.exists("1/a.bin")).isTrue();
        assertThatThrownBy(() -> s.store("../evil.bin", new byte[] {1})).isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> s.open("../../etc/passwd")).isInstanceOf(SecurityException.class);
    }
}
