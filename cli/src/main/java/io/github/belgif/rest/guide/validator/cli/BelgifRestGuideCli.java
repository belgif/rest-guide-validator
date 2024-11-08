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
import java.util.Scanner;
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

    /**
     * postInstall flag is used by the windows installer to start the application after installation.
     * This will show a welcome message.
     */
    @CommandLine.Option(names = {"--postInstall"}, hidden = true)
    private boolean postInstall;

    @Override
    public Integer call() {
        if (postInstall) {
            printPostInstall();
            return 0;
        } else {
            log.info("Using: belgif-rest-guide-validator-{}", VersionProvider.getValidatorVersion());
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
    }

    public static void main(String[] args) {
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
        log.info(runner.listOptions());
        log.info("Starting OpenApi validation");
        return runner.executeRules();
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

    private void printPostInstall() {
        log.info("belgif-rest-guide-validator-{} successfully installed!", VersionProvider.getValidatorVersion());
        log.info("\nUse command: 'belgif-validate-openapi' followed by a file name or path to start.");
        log.info("Use 'belgif-validate-openapi --help' for all options.");
        log.info("\n\n========================");
        log.info("\nPress Enter to exit...");
        new Scanner(System.in).nextLine();
    }

}
