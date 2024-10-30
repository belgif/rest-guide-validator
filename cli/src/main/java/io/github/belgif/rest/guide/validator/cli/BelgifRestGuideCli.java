package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.cli.options.ValidatorOptions;
import io.github.belgif.rest.guide.validator.cli.util.VersionProvider;
import io.github.belgif.rest.guide.validator.runner.ValidationRunner;
import io.github.belgif.rest.guide.validator.runner.output.OutputGroupBy;
import io.github.belgif.rest.guide.validator.runner.output.OutputProcessor;
import io.github.belgif.rest.guide.validator.runner.output.OutputType;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


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

    private List<File> filesToProcess = new ArrayList<>();

    private OutputGroupBy groupBy;

    private File jsonOutputFile;

    private List<OutputType> outputTypes;

    private Set<OutputProcessor> outputProcessors;

    @Override
    public void run() {
        try {
            init();
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

    private boolean executeRules() {
        log.info("Starting OpenApi validation");
        return ValidationRunner.executeRules(filesToProcess, options.getExcludedFiles(), outputProcessors);
    }

    private void init() throws FileNotFoundException {
        this.groupBy = OutputGroupBy.fromString(options.getGroupBy());
        this.jsonOutputFile = options.getJsonOutputFile();
        initOutputTypes();
        outputProcessors = ValidationRunner.buildOutputProcessors(outputTypes, this.groupBy, options.getOutputDir(), jsonOutputFile);
        filesToProcess = ValidationRunner.buildFilesToProcess(options.getFiles());
    }

    private static void printCommandLineArguments(String[] args) {
        log.info("Using: belgif-rest-guide-validator-{}", VersionProvider.getValidatorVersion());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i]).append("\t");
        }
        log.info("Options: {}", sb.toString());
    }

    private void initOutputTypes() {
        List<OutputType> types = new ArrayList<>();
        for (String type : options.getOutputTypes()) {
            types.add(OutputType.valueOf(type.toUpperCase()));
        }
        this.outputTypes = types;
    }

}
