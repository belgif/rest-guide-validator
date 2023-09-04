package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class ShouldUseProblemResponseTest extends AbstractOasRuleTest {
    protected String ruleName = "[prb-defaul]";
    int errorCount = 3;

    public ShouldUseProblemResponseTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
    }
}
