package be.belgium.gcloud.rest.styleguide.validation.rules;

import be.belgium.gcloud.rest.styleguide.validation.OpenApiValidator;
import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Slf4j
public abstract class AbstractOasRuleTest extends AbstractRuleTest{

    /** TODO: merge with parent class **/
    protected OpenApiViolationAggregator callRules(String fileName) throws IOException {
        var file = new File(getClass().getResource(lowerCaseFirstStripTestSuffix(getClass().getSimpleName()) + fileName).getFile());
        return OpenApiValidator.callRuleOAS(file, List.of());
    }

}
