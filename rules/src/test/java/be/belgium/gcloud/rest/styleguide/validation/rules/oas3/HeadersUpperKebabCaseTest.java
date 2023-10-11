package be.belgium.gcloud.rest.styleguide.validation.rules.oas3;

import be.belgium.gcloud.rest.styleguide.validation.core.ViolationType;
import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class HeadersUpperKebabCaseTest extends AbstractOasRuleTest {
    protected String ruleName = "[hdr-case]";
    protected ViolationType violationType = ViolationType.MANDATORY;

    int errorCount = 4;

    public HeadersUpperKebabCaseTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
        this.setViolationType(violationType);
    }
}
