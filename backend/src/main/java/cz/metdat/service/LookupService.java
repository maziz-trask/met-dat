package cz.metdat.service;

import cz.metdat.config.AppProperties;
import cz.metdat.domain.AppUser;
import cz.metdat.domain.TestResult;
import cz.metdat.domain.TestStatus;
import cz.metdat.dto.Dtos.Lookups;
import cz.metdat.repository.AppUserRepository;
import cz.metdat.repository.TechnicalTestRepository;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Code lists served to the UI so nothing (types, statuses, departments) is hard-coded in React. */
@Service
public class LookupService {
    private final TechnicalTestRepository tests;
    private final AppUserRepository users;
    private final AppProperties props;

    public LookupService(TechnicalTestRepository tests, AppUserRepository users, AppProperties props) {
        this.tests = tests; this.users = users; this.props = props;
    }

    @Transactional(readOnly = true)
    public Lookups lookups() {
        List<AppUser> active = users.findAllByActiveTrueOrderByRoleAscNameAsc();
        TreeSet<String> departments = new TreeSet<>(tests.distinctDepartments());
        active.stream().map(AppUser::getDepartment).filter(d -> d != null && !d.isBlank()).forEach(departments::add);
        return new Lookups(
            Arrays.stream(TestStatus.values()).map(TestStatus::label).toList(),
            Arrays.stream(TestResult.values()).map(TestResult::label).toList(),
            List.copyOf(departments),
            active.stream().map(AppUser::getName).sorted().toList(),
            props.upload().allowedExtensions(), props.upload().maxFileSize().toBytes());
    }
}
