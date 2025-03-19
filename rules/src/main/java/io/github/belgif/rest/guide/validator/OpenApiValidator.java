package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.ViolationReport;
import jakarta.enterprise.inject.spi.CDI;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieRuntimeBuilder;
import org.kie.api.runtime.StatelessKieSession;

import java.io.File;
import java.util.List;

/**
 * Validate an open-api file using Drools and rules from the classpath.
 */
@Slf4j
public class OpenApiValidator {
    private final StatelessKieSession kSession;

    public OpenApiValidator() {
        StatelessKieSession kieSession;
        try { // when running in quarkus
            var kieRuntimeBuilder = CDI.current().select(KieRuntimeBuilder.class).get();
            kieSession = kieRuntimeBuilder.getKieBase().newStatelessKieSession();
        } catch(RuntimeException e){
            var kieServices = KieServices.Factory.get();
            var kContainer = kieServices.getKieClasspathContainer();
            kieSession = kContainer.newStatelessKieSession();
        }
        this.kSession = kieSession;
    }

    public ViolationReport callRules(File openApiFile, List<String> excludedFiles) {
        return RuleRunner.execute(openApiFile, excludedFiles, kSession);
    }
}
