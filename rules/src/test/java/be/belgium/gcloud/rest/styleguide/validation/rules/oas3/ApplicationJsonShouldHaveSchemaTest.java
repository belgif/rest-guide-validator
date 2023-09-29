package be.belgium.gcloud.rest.styleguide.validation.rules.oas3;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class ApplicationJsonShouldHaveSchemaTest extends AbstractOasRuleTest {
    protected String ruleName = "[evo-object]";
    int errorCount = 2;

    public ApplicationJsonShouldHaveSchemaTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
    }
}
