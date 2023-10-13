package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.ViolationType;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.drools.core.base.RuleNameStartsWithAgendaFilter;
import org.drools.model.Rule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.command.Command;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.rule.AgendaFilter;
import org.kie.internal.command.CommandFactory;
import org.kie.internal.io.ResourceFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validate an open-api file using Drools and rules from the classpath.
 */
@Slf4j
public class OpenApiValidator {
    private static KieServices kieServices = KieServices.Factory.get();
    private static KieContainer kContainer = kieServices.getKieClasspathContainer();

    /**
     * Validate the file and use the outputProcessor. Return true if the file is a valid open-api file.
     *
     * @param file
     * @param excludedPaths
     * @param outputProcessors
     * @return
     */
    public static boolean isOasValid(@NotNull File file, List<String> excludedPaths, @Nullable OutputProcessor... outputProcessors) {
        var oas = callRuleOAS(file,excludedPaths);

        if (outputProcessors != null) for (OutputProcessor outputProcessor : outputProcessors) {
            outputProcessor.process(oas);
        }
        return oas.getViolations().stream().filter(violation -> violation.type == ViolationType.MANDATORY).collect(Collectors.toList()).isEmpty();
    }

    public static OpenApiViolationAggregator callRuleOAS(File openApiFile, List<String> excludedPaths) {
        var kSession = kContainer.newStatelessKieSession();
        var oas = RuleRunner.execute(openApiFile, excludedPaths, kSession);
        return oas;
    }
}
