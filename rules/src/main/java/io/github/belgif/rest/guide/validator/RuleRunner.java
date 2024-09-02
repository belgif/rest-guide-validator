package io.github.belgif.rest.guide.validator;

import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.command.CommandFactory;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RuleRunner {

    private RuleRunner() {
    }

    public static OpenApiViolationAggregator execute(@NotNull File openApiFile, List<String> excludedFiles, StatelessKieSession kSession) {
        var oas = new OpenApiViolationAggregator();
        oas.setRuleNumber(kSession.getKieBase().getKiePackages().stream().mapToInt(pack -> pack.getRules().size()).sum());
        oas.setExcludedFiles(excludedFiles);

        var parserResult = new Parser(openApiFile).parse(oas);
        if (parserResult == null) {
            return oas;
        }

        kSession.setGlobal("oas", oas);
        kSession.setGlobal("parserResult", parserResult);
        var start = System.currentTimeMillis();
        List<Command> commands = new ArrayList<>();
        commands.add(CommandFactory.newInsertElements(parserResult.getAllDefinitions()));
        commands.add(CommandFactory.newInsert(parserResult.getOpenAPI()));
        kSession.execute(CommandFactory.newBatchExecution(commands));
        oas.setTime((System.currentTimeMillis() - start) / 1000f);

        return oas;
    }
}
