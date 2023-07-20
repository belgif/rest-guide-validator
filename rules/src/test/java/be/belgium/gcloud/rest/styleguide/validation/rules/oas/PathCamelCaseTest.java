package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;

@Getter
public class PathCamelCaseTest extends AbstractOasRuleTest {
    int errorCount = 3;
    private String ruleName = "[uri-notat]";
}
