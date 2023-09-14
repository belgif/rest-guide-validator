package be.belgium.gcloud.rest.styleguide.validation.rules.oas3;

import be.belgium.gcloud.rest.styleguide.validation.core.ViolationType;
import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class CodesShouldBeLowerCamelCaseTest extends AbstractOasRuleTest {
    protected String ruleName = "[cod-design]";
    protected ViolationType violationType = ViolationType.MANDATORY;

    int errorCount = 2;

    public CodesShouldBeLowerCamelCaseTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
        this.setViolationType(violationType);
    }
}
