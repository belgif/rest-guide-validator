package be.belgium.gcloud.rest.styleguide.validation.rules.oas;

import be.belgium.gcloud.rest.styleguide.validation.rules.AbstractOasRuleTest;
import lombok.Getter;

@Getter
public class PathTrailingSlashTest extends AbstractOasRuleTest {
    private String ruleName = "[uri-notat]";
}
