package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.cli.options.ValidatorOptions;
import io.github.belgif.rest.guide.validator.cli.util.VersionProvider;
import io.github.belgif.rest.guide.validator.runner.ValidationRunner;
import io.github.belgif.rest.guide.validator.runner.output.OutputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


@CommandLine.Command(name = "belgif-validate-openapi",
        description = "Validate if an OpenAPI document conforms to the guidelines in the Belgif REST guide.",
        footerHeading = "For additional information: ",
        footer = "https://github.com/belgif/rest-guide-validator/blob/main/readme.md",
        mixinStandardHelpOptions = true,
        showDefaultValues = true)
public class BelgifRestGuideCli implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(BelgifRestGuideCli.class);

    @CommandLine.Mixin
    private ValidatorOptions options;

    @Override
    public Integer call() {
        try {
            if (executeRules()) {
                return 0;
            } else {
                return 11;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return 1;
        }
    }

    public static void main(String[] args) {
        printCommandLineArguments(args);
        int exitCode = new CommandLine(new BelgifRestGuideCli()).execute(args);
        System.exit(exitCode);
    }

    private boolean executeRules() throws FileNotFoundException {
        ValidationRunner runner = ValidationRunner.builder()
                .files(options.getFiles())
                .excludedFiles(options.getExcludedFiles())
                .jsonOutputFile(options.getJsonOutputFile())
                .outputDir(options.getOutputDir())
                .outputTypes(initOutputTypes())
                .groupBy(options.getGroupBy())
                .build();
        log.info("Validating with the following options: \n{}", runner.listOptions());
        log.info("Starting OpenApi validation");
        return runner.executeRules();
    }

    private static void printCommandLineArguments(String[] args) {
        log.info("Using: belgif-rest-guide-validator-{}", VersionProvider.getValidatorVersion());
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg).append("\t");
        }
        log.info("Arguments: {}", sb);
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
