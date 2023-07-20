package be.belgium.gcloud.rest.styleguide.validation.rules.oas3;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;

public class ServerUrlFormatTest extends AbstractOasRuleTest {
    protected String ruleName = "[uri-format]";

    public ServerUrlFormatTest() {
        this.setRuleName(ruleName);
    }
}
