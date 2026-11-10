package cz.metdat;

import static org.assertj.core.api.Assertions.assertThat;

import cz.metdat.domain.*;
import cz.metdat.dto.TestFilter;
import cz.metdat.repository.*;
import cz.metdat.service.TestSpecifications;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifies the Flyway-imported workbook data (repository + spec layer against real PostgreSQL). */
class SeedDataTest extends AbstractIntegrationTest {
    @Autowired TechnicalTestRepository tests;
    @Autowired TestTypeRepository types;
    @Autowired AppUserRepository users;
    @Autowired AttachmentRepository attachments;
    @Autowired JdbcTemplate jdbc;

    @Test void workbookCounts() {
        assertThat(tests.count()).isEqualTo(50);
        assertThat(types.count()).isEqualTo(11);
        assertThat(types.findAll().stream().filter(TestType::isActive)).hasSize(10);
        assertThat(users.count()).isEqualTo(10);
        assertThat(users.findAll().stream().filter(u -> u.getRole() == Role.EDITOR)).hasSize(6);
        assertThat(users.findAll().stream().filter(u -> u.getRole() == Role.READER)).hasSize(4);
        assertThat(attachments.count()).isEqualTo(39);
        assertThat(attachments.findAll().stream().allMatch(a -> a.getFileSize() > 0)).as("synthetic files generated").isTrue();
    }

    @Test void legacyCompletedExceptionsPreserved() {
        long bad = tests.findAll().stream().filter(t -> t.getStatus() == TestStatus.COMPLETED && !(t.getResult() != null && t.getResult().satisfiesCompletion())).count();
        assertThat(bad).isEqualTo(13);
        assertThat(tests.findAll().stream().filter(t -> t.getStatus() == TestStatus.PLANNED)).hasSize(10);
        assertThat(tests.findAll().stream().filter(t -> t.getStatus() == TestStatus.CANCELLED)).hasSize(4);
    }

    @Test void calendarWeeksPreservedAndPeriodsAreIsoMondayToSunday() {
        TechnicalTest t = tests.findAll().stream().filter(x -> x.getTestNumber().equals("TEST-2026-0001")).findFirst().orElseThrow();
        assertThat(t.getWeekLabel()).isEqualTo("KT08/26");
        assertThat(t.getTestDate()).isNull();
        assertThat(t.getPeriodStart()).isEqualTo(LocalDate.of(2026, 2, 16));
        assertThat(t.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 2, 22));
        assertThat(t.getCreatedBy()).isEqualTo("filip.vesely@example.com");
    }

    @Test void filtersAreExecutedInSql() {
        assertThat(tests.count(TestSpecifications.of(new TestFilter("00000007", null, null, null, null)))).isEqualTo(1);
        assertThat(tests.count(TestSpecifications.of(new TestFilter(null, null, TestStatus.IN_PROGRESS, null, null)))).isEqualTo(8);
        // week KT08/26 = 2026-02-16..2026-02-22: overlapping range matches, adjacent day does not
        assertThat(tests.count(TestSpecifications.of(new TestFilter("TMBTST26A00000001", null, null, LocalDate.of(2026, 2, 22), LocalDate.of(2026, 2, 28))))).isEqualTo(1);
        assertThat(tests.count(TestSpecifications.of(new TestFilter("TMBTST26A00000001", null, null, LocalDate.of(2026, 2, 23), LocalDate.of(2026, 2, 28))))).isZero();
        assertThat(tests.count(TestSpecifications.of(new TestFilter("TMBTST26A00000001", null, null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 15))))).isZero();
    }

    @Test void legacyTypeIsUnusedAndDatabaseConstraintsHold() {
        assertThat(types.usageCounts().stream().anyMatch(r -> r[0].equals(11L))).isFalse();
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class,
            () -> jdbc.update("UPDATE technical_test SET status = 'Bogus' WHERE id = 1"));
        org.junit.jupiter.api.Assertions.assertThrows(org.springframework.dao.DataAccessException.class,
            () -> jdbc.update("DELETE FROM test_type WHERE id = 1"));   // FK: types referenced by tests cannot be removed
    }
}
