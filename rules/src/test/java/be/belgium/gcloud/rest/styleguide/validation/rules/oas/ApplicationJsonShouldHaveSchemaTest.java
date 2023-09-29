package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class ApplicationJsonShouldHaveSchemaTest extends AbstractOasRuleTest {
    protected String ruleName = "[evo-object]";
    int errorCount = 1;

    public ApplicationJsonShouldHaveSchemaTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
    }
}
