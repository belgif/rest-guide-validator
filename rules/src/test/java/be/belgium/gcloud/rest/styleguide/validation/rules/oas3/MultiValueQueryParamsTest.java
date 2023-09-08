package be.belgium.gcloud.rest.styleguide.validation.rules.oas3;

import be.belgium.gcloud.rest.styleguide.validation.core.ViolationType;
import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class MultiValueQueryParamsTest extends AbstractOasRuleTest {
    protected String ruleName = "[qry-multi]";
    protected ViolationType violationType = ViolationType.MANDATORY;

    int errorCount = 2;

    public MultiValueQueryParamsTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
        this.setViolationType(violationType);
    }
}
