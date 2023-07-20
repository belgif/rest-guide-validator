package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;

@Getter
public class JsonPropertiesCamelCaseTest extends AbstractOasRuleTest {
    int errorCount = 3;
    private String ruleName = "[jsn-naming]";
}
