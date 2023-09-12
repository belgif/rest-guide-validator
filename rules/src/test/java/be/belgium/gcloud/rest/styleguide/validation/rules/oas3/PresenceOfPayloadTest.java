package be.belgium.gcloud.rest.styleguide.validation.rules.oas3;

import be.belgium.gcloud.rest.styleguide.validation.core.ViolationType;
import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class PresenceOfPayloadTest extends AbstractOasRuleTest {

    protected String ruleName = "[presenceOfPayload]";
    int errorCount = 8;
    protected ViolationType violationType = ViolationType.MANDATORY;

    public PresenceOfPayloadTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
        this.setViolationType(violationType);
    }
}
