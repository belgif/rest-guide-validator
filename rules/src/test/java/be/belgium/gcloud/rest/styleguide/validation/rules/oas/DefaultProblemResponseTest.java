package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.core.ViolationType;
import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class DefaultProblemResponseTest extends AbstractOasRuleTest {
    protected String ruleName = "[prb-defaul]";
    protected ViolationType violationType = ViolationType.MANDATORY;

    int errorCount = 4;

    public DefaultProblemResponseTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
        this.setViolationType(violationType);
    }
}
