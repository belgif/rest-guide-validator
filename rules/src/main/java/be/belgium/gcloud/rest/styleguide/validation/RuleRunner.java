package be.belgium.gcloud.rest.styleguide.validation;

import be.belgium.gcloud.rest.styleguide.validation.core.ApiFunctions;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import be.belgium.gcloud.rest.styleguide.validation.core.parser.Parser;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RuleRunner {
    public static OpenApiViolationAggregator execute(@NotNull File openApiFile, List<String> excludedPaths, StatelessKieSession kSession) {
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

        var excluded = oas.getViolations().stream().filter(violation -> {
            if (violation.getLineNumber().getFileName().equals(oas.getOpenApiFile().getName())) {
                return ApiFunctions.isInPathList(parserResult.getPaths(), excludedPaths, violation.getLineNumber().getLineNumber());
            } else {
                return false;
            }
        }).collect(Collectors.toSet());

        oas.getViolations().removeAll(excluded);

        return oas;
    }
}
