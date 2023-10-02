package be.belgium.gcloud.rest.styleguide.validation.rules.oas3;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class JsonObjectAsTopLevelStructureTest extends AbstractOasRuleTest {
    protected String ruleName = "[evo-object]";
    int errorCount = 5;

    public JsonObjectAsTopLevelStructureTest() {
        this.setRuleName(ruleName);
        this.setErrorCount(errorCount);
    }
}
