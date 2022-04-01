package be.belgium.gcloud.rest.styleguide.validation.rules;

import be.belgium.gcloud.rest.styleguide.validation.OpenApiValidator;
import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Getter
@Slf4j
public abstract class AbstractOasRuleTest extends AbstractRuleTest{

    protected OpenApiViolationAggregator callRules(String fileName) throws IOException {
        var file = new File(getClass().getResource(lowerCaseFirstStripTestSuffix(getClass().getSimpleName()) + fileName).getFile());
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        var openApi = ApiFunctions.buildOpenApiSpecification(file, openApiViolationAggregator);
        OpenApiValidator.callRuleOAS(openApiViolationAggregator, openApi);
        return openApiViolationAggregator;
    }
}
