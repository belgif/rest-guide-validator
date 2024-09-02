package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.ViolationType;
import io.github.belgif.rest.guide.validator.output.OutputProcessor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.runtime.KieContainer;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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

    /**
     * Validate the file and use the outputProcessor. Return true if the file is a valid open-api file.
     *
     * @param file
     * @param outputProcessors
     * @return
     */
    public static boolean isOasValid(@NotNull File file, List<String> excludedFiles, @Nullable OutputProcessor... outputProcessors) {
        var oas = callRuleOAS(file, excludedFiles);

        if (outputProcessors != null) for (OutputProcessor outputProcessor : outputProcessors) {
            outputProcessor.process(oas);
        }
        return oas.getActionableViolations().stream().noneMatch(violation -> violation.getType() == ViolationType.MANDATORY);
    }

    public static OpenApiViolationAggregator callRuleOAS(File openApiFile, List<String> excludedFiles) {
        var kSession = kContainer.newStatelessKieSession();
        return RuleRunner.execute(openApiFile, excludedFiles, kSession);
    }
}
