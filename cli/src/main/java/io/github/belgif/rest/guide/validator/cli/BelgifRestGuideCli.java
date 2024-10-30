package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.cli.options.ValidatorOptions;
import io.github.belgif.rest.guide.validator.cli.util.VersionProvider;
import io.github.belgif.rest.guide.validator.runner.RunnerOptions;
import io.github.belgif.rest.guide.validator.runner.ValidationRunner;
import io.github.belgif.rest.guide.validator.runner.output.OutputType;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@CommandLine.Command(name = "belgif-validate-openapi",
        description = "The belgif-rest-guide-validator Maven plugin is used to validate if an OpenAPI document conforms to the guidelines in the Belgif REST guide.",
        footerHeading = "For additional information: ",
        footer = "https://github.com/belgif/rest-guide-validator/blob/main/readme.md",
        mixinStandardHelpOptions = true,
        showDefaultValues = true)
public class BelgifRestGuideCli implements Runnable {

    @CommandLine.Mixin
    private ValidatorOptions options;

    private RunnerOptions runnerOptions;

    @Override
    public void run() {
        try {
            runnerOptions = RunnerOptions.builder()
                    .files(options.getFiles())
                    .excludedFiles(options.getExcludedFiles())
                    .jsonOutputFile(options.getJsonOutputFile())
                    .outputDir(options.getOutputDir())
                    .outputTypes(initOutputTypes())
                    .groupBy(options.getGroupBy())
                    .build();
            if (executeRules()) {
                System.exit(0);
            } else {
                System.exit(11);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        printCommandLineArguments(args);
        int exitCode = new CommandLine(new BelgifRestGuideCli()).execute(args);
        System.exit(exitCode);
    }

    private boolean executeRules() throws FileNotFoundException {
        log.info("Starting OpenApi validation");
        return ValidationRunner.executeRules(runnerOptions);
    }

    private static void printCommandLineArguments(String[] args) {
        log.info("Using: belgif-rest-guide-validator-{}", VersionProvider.getValidatorVersion());
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg).append("\t");
        }
        log.info("Options: {}", sb);
    }

    /**
     * Makes sure that the enum can be selected case-insensitive in the CLI.
     */
    private List<OutputType> initOutputTypes() {
        List<OutputType> types = new ArrayList<>();
        for (String type : options.getOutputTypes()) {
            types.add(OutputType.valueOf(type.toUpperCase()));
        }
        return types;
    }

}
