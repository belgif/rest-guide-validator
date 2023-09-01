package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;

@Getter
public class RepresentationOfCollectionTest extends AbstractOasRuleTest {
    private String ruleName = "[col-repres]";
    int errorCount = 4;
}
