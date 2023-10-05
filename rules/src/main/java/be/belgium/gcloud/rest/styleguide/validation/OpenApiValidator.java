package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.ViolationType;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.command.CommandFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.File;
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

    private OpenApiValidator() {
    }

    public static OpenApiViolationAggregator callRuleOAS(@NotNull File openApiFile, List<String> excludedPaths) {
        var kSession = kContainer.newStatelessKieSession();
        var oas = new OpenApiViolationAggregator();
        oas.setRuleNumber(kSession.getKieBase().getKiePackages().stream().mapToInt(pack -> pack.getRules().size()).sum());

        var parserResult = new Parser(openApiFile).parse(oas);
        if (parserResult == null) {
            return oas;
        }

        kSession.setGlobal("oas", oas);
        kSession.setGlobal("jsonString", parserResult.getJsonString());
        kSession.setGlobal("parserResult", parserResult);
        var start = System.currentTimeMillis();
        List<Command> commands = new ArrayList<>();
        commands.add(CommandFactory.newInsertElements(parserResult.getAllDefinitions()));
        commands.add(CommandFactory.newInsert(parserResult.getOpenAPI()));
        kSession.execute(CommandFactory.newBatchExecution(commands));
        oas.setTime((System.currentTimeMillis() - start) / 1000f);

        var excluded = oas.getViolations().stream().filter(violation -> ApiFunctions.isInPathList(parserResult.getPaths(), excludedPaths, violation.getLineNumber())).collect(Collectors.toSet());
        oas.getViolations().removeAll(excluded);

        return oas;
    }

    /**
     * Validate the file and use the outputProcessor. Return true if the file is a valid open-api file.
     *
     * @param file
     * @param excludedPaths
     * @param outputProcessors
     * @return
     */
    public static boolean isOasValid(@NotNull File file, List<String> excludedPaths, @Nullable OutputProcessor... outputProcessors) {
        var oas = callRuleOAS(file, excludedPaths);

        if (outputProcessors != null) for (OutputProcessor outputProcessor : outputProcessors) {
            outputProcessor.process(oas);
        }
        return oas.getViolations().stream().filter(violation -> violation.type == ViolationType.MANDATORY).collect(Collectors.toList()).isEmpty();
    }
}
