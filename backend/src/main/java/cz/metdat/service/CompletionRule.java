package cz.metdat.service;

import cz.metdat.domain.TestResult;
import cz.metdat.exception.BusinessRuleException;
import java.util.ArrayList;
import java.util.List;

/** F06: a test may only be Completed when its result is exactly Passed or Failed AND it has at least one attachment. */
public final class CompletionRule {
    private CompletionRule() {}

    public static final String CODE = "COMPLETION_REQUIREMENTS_NOT_MET";

    public static void check(TestResult result, long attachmentCount) {
        List<String> problems = new ArrayList<>();
        if (result == null || !result.satisfiesCompletion()) {
            problems.add("the result must be Passed or Failed (current result: " + (result == null ? "empty" : result.label()) + ")");
        }
        if (attachmentCount < 1) problems.add("at least one attachment must be uploaded");
        if (!problems.isEmpty()) {
            throw new BusinessRuleException(CODE, "The test cannot be set to Completed: " + String.join(" and ", problems) + ".");
        }
    }
}
