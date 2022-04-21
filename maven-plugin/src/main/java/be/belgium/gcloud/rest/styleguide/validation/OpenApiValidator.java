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

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Validate an open-api file using Drools and rules from the classpath.
 */
@Slf4j
public class OpenApiValidator {
    private static KieServices kieServices = KieServices.Factory.get();
    private static KieContainer kContainer = kieServices.getKieClasspathContainer();

    private OpenApiValidator(){}

    static void callRuleOAS(@NotNull OpenApiViolationAggregator oas, @NotNull OpenAPI openApi) throws JsonProcessingException {
        var kSession = kContainer.newStatelessKieSession();
        oas.setRuleNumber(kSession.getKieBase().getKiePackages().stream().mapToInt(pack-> pack.getRules().size()).sum());

        kSession.setGlobal("oas", oas);
        kSession.setGlobal("jsonString", getJsonString(oas));
        var start = System.currentTimeMillis();
        kSession.execute(openApi);
        oas.setTime((System.currentTimeMillis()-start)/1000f);
    }

    private static String getJsonString(OpenApiViolationAggregator oas) throws JsonProcessingException {
        var yamlReader = new ObjectMapper(new YAMLFactory());
        var obj = yamlReader.readValue(oas.getSrc().stream().collect(Collectors.joining("\n")), Object.class);
        return new ObjectMapper().writeValueAsString(obj);
    }

    /**
     * Validate the file without using a outputProcessor. Return true if the file is a valid open-api file.
     * @param file
     * @return
     */
    public static boolean isOasValid(@NotNull File file) {
        return isOasValid(file, null);
    }

    /**
     * Validate the file and use the outputProcessor. Return true if the file is a valid open-api file.
     * @param file
     * @param outputProcessors
     * @return
     */
    public static boolean isOasValid(@NotNull File file, @Nullable OutputProcessor... outputProcessors) {
        OpenApiViolationAggregator openApiViolationAggregator = null;
        try {
            openApiViolationAggregator = new OpenApiViolationAggregator();
            var openApi = ApiFunctions.buildOpenApiSpecification(file, openApiViolationAggregator);

            callRuleOAS(openApiViolationAggregator, openApi);
        } catch (IOException e) {
            openApiViolationAggregator.addViolation(e.getClass().getSimpleName(), e.getLocalizedMessage());
        }

        if(outputProcessors != null)
            for (OutputProcessor outputProcessor : outputProcessors) {
                outputProcessor.process(openApiViolationAggregator);
            }
        return openApiViolationAggregator.getViolations().isEmpty();
    }
}
