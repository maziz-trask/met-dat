package cz.metdat.service;

import cz.metdat.domain.TestResult;
import cz.metdat.exception.BusinessRuleException;
import cz.metdat.i18n.MsgRef;

/** F06: a test may only be Completed when its result is exactly Passed or Failed AND it has at least one attachment. */
public final class CompletionRule {
    private CompletionRule() {}

    public static final String CODE = "COMPLETION_REQUIREMENTS_NOT_MET";

    public static void check(TestResult result, long attachmentCount) {
        boolean resultOk = result != null && result.satisfiesCompletion();
        boolean attachmentOk = attachmentCount >= 1;
        if (resultOk && attachmentOk) return;
        MsgRef current = new MsgRef(result == null ? "result.none" : "result." + result.name());
        if (!resultOk && !attachmentOk) throw new BusinessRuleException(CODE, "completion.both", current);
        if (!resultOk) throw new BusinessRuleException(CODE, "completion.result", current);
        throw new BusinessRuleException(CODE, "completion.attachment");
    }
}
