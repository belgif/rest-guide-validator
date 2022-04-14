package be.belgium.gcloud.rest.styleguide.validation.rules;

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
import java.util.stream.Collectors;

@Getter
@Slf4j
public abstract class AbstractOasRuleTest extends AbstractRuleTest{
    private String groupId = "be.belgium.gcloud.rest";
    private String artifactId = "rest-styleguide-validation-rules";
    private String version = "1.0.0-SNAPSHOT";

    protected OpenApiViolationAggregator callRules(String fileName) throws IOException {
        var file = new File(getClass().getResource(lowerCaseFirstStripTestSuffix(getClass().getSimpleName()) + fileName).getFile());
        var oas = new OpenApiViolationAggregator();
        var openApi = ApiFunctions.buildOpenApiSpecification(file, oas);

        var kieServices = KieServices.Factory.get();
        var kContainer = kieServices.getKieClasspathContainer();

        var kSession = kContainer.newStatelessKieSession(); //.newKieSession();
        oas.setRuleNumber(kSession.getKieBase().getKiePackages().stream().mapToInt(pack-> pack.getRules().size()).sum());

        kSession.setGlobal("oas", oas);
        kSession.setGlobal("jsonString", getJsonString(oas));
        long start = System.currentTimeMillis();
        kSession.execute(openApi);
        oas.setTime((System.currentTimeMillis()-start)/1000f);

        return oas;
    }

    String getJsonString(OpenApiViolationAggregator oas) throws JsonProcessingException {
        var yamlReader = new ObjectMapper(new YAMLFactory());
        var obj = yamlReader.readValue(oas.getSrc().stream().collect(Collectors.joining("\n")), Object.class);
        return new ObjectMapper().writeValueAsString(obj);
    }

    void callRuleEngine(Object o) {
        KieServices ks = KieServices.Factory.get();
        KieContainer kContainer = ks.getKieClasspathContainer();
        KieSession kieSession = kContainer.newKieSession();

        kieSession.insert(o);
        kieSession.setGlobal("tx", o);
        kieSession.fireAllRules();
    }

}
