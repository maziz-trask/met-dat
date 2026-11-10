package cz.metdat.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "technical_test")
public class TechnicalTest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "test_number", nullable = false, updatable = false, length = 20)
    private String testNumber;
    @Column(nullable = false, length = 17)
    private String vin;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_type_id", nullable = false)
    private TestType testType;
    @Column(nullable = false, length = 20)
    private String department;
    @Column(name = "test_year", nullable = false) private int testYear;
    @Column(name = "test_week", nullable = false) private int testWeek;
    @Column(name = "test_date") private LocalDate testDate;
    @Column(name = "period_start", nullable = false) private LocalDate periodStart;
    @Column(name = "period_end", nullable = false) private LocalDate periodEnd;
    @Column(name = "responsible_person", nullable = false, length = 150)
    private String responsiblePerson;
    @Convert(converter = TestStatus.DbConverter.class)
    @Column(nullable = false, length = 20)
    private TestStatus status;
    @Convert(converter = TestResult.DbConverter.class)
    @Column(length = 30)
    private TestResult result;
    @Column(columnDefinition = "text")
    private String comment;
    @Column(name = "created_by", nullable = false, updatable = false) private String createdBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "modified_by") private String modifiedBy;
    @Column(name = "modified_at") private Instant modifiedAt;
    @Version private long version;

    /** Calendar week label in the workbook format, e.g. KT08/26. */
    public String getWeekLabel() { return "KT%02d/%02d".formatted(testWeek, testYear % 100); }

    public Long getId() { return id; }
    public String getTestNumber() { return testNumber; }
    public void setTestNumber(String testNumber) { this.testNumber = testNumber; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public TestType getTestType() { return testType; }
    public void setTestType(TestType testType) { this.testType = testType; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public int getTestYear() { return testYear; }
    public void setTestYear(int testYear) { this.testYear = testYear; }
    public int getTestWeek() { return testWeek; }
    public void setTestWeek(int testWeek) { this.testWeek = testWeek; }
    public LocalDate getTestDate() { return testDate; }
    public void setTestDate(LocalDate testDate) { this.testDate = testDate; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }
    public TestStatus getStatus() { return status; }
    public void setStatus(TestStatus status) { this.status = status; }
    public TestResult getResult() { return result; }
    public void setResult(TestResult result) { this.result = result; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
    public Instant getModifiedAt() { return modifiedAt; }
    public void setModifiedAt(Instant modifiedAt) { this.modifiedAt = modifiedAt; }
    public long getVersion() { return version; }
}
