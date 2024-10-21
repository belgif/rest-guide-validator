package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;

import java.io.File;
import java.util.List;

/**
 * Validate an open-api file using Drools and rules from the classpath.
 */
@Slf4j
public class OpenApiValidator {
    private static final KieServices kieServices = KieServices.Factory.get();
    private static final KieContainer kContainer = kieServices.getKieClasspathContainer();

    private OpenApiValidator() {
    }

    public static ViolationReport callRules(File openApiFile, List<String> excludedFiles) {
        var kSession = kContainer.newStatelessKieSession();
        return RuleRunner.execute(openApiFile, excludedFiles, kSession);
    }
}
