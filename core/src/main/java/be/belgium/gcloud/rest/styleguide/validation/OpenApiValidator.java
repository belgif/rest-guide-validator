package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.stream.Collectors;

@Slf4j
public class OpenApiValidator {
    private static KieServices kieServices = KieServices.Factory.get();
    private static KieContainer kContainer = kieServices.getKieClasspathContainer();

    private OpenApiValidator(){}

    public static void callRuleOAS(OpenApiViolationAggregator oas, OpenAPI openApi) throws JsonProcessingException {
        var kSession = kContainer.newStatelessKieSession();
        kSession.setGlobal("oas", oas);
        kSession.setGlobal("jsonString", getJsonString(oas));
        kSession.execute(openApi);
    }

    private static String getJsonString(OpenApiViolationAggregator oas) throws JsonProcessingException {
        var yamlReader = new ObjectMapper(new YAMLFactory());
        var obj = yamlReader.readValue(oas.getSrc().stream().collect(Collectors.joining("\n")), Object.class);
        return new ObjectMapper().writeValueAsString(obj);
    }

    public static boolean isOasValid(File file) throws IOException {
        var openApiViolationAggregator = new OpenApiViolationAggregator();
        var openApi = ApiFunctions.buildOpenApiSpecification(file, openApiViolationAggregator);
        callRuleOAS(openApiViolationAggregator, openApi);
        printViolation(openApiViolationAggregator);
        return openApiViolationAggregator.getViolations().isEmpty();
    }

    private static void printViolation(OpenApiViolationAggregator apiDetail){
        Collections.sort(apiDetail.getViolations());
        if( ! apiDetail.getViolations().isEmpty()){
            log.debug("\n {} OpenApi Violations for: "+apiDetail.getOpenApiFile().getAbsolutePath(), apiDetail.getViolations().size());
        }
        apiDetail.getViolations().forEach(v->{
            switch (v.type){
                case MANDATORY:
                    log.error(v.toString()); break;
                case RECOMMENDED:
                    log.warn(v.toString()); break;
                case STYLE:
                    log.debug(v.toString()); break;
                default:
                    log.info(v.toString());
            }
        });
    }
}
