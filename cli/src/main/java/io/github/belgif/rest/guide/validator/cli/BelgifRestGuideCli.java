package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.cli.options.ValidatorOptions;
import io.github.belgif.rest.guide.validator.cli.util.VersionProvider;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.input.InputFileUtil;
import io.github.belgif.rest.guide.validator.output.*;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


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

    private final List<File> filesToProcess = new ArrayList<>();

    private OutputGroupBy groupBy;

    private File jsonOutputFile;

    private List<OutputType> outputTypes;

    private Set<OutputProcessor> outputProcessors;

    @Override
    public void run() {
        try {
            init();
            if (isOpenApiValid()) {
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

    private boolean isOpenApiValid() {
        log.info("Starting OpenApi validation");
        var isValid = new AtomicBoolean(true);
        var violationReports = filesToProcess.stream().map(file -> OpenApiValidator.callRules(file, options.getExcludedFiles())).toList();
        isValid.set(violationReports.stream().allMatch(ViolationReport::isOasValid));
        outputProcessors.forEach(processor -> processor.process(new ViolationReport(violationReports)));
        return isValid.get();
    }

    private void init() throws FileNotFoundException {
        this.groupBy = OutputGroupBy.fromString(options.getGroupBy());
        this.jsonOutputFile = options.getJsonOutputFile();
        initOutputProcessor();
        initFiles();
    }

    private void initFiles() throws FileNotFoundException {
        if (options.getFiles().isEmpty())
            throw new IllegalArgumentException("rest-guide-validator needs at least one file ! Set the '-f' or '--files' parameter.");
        Optional<File> fileNotFound = options.getFiles().stream().filter(file -> !file.exists()).findAny();
        if (fileNotFound.isPresent()) {
            throw new FileNotFoundException("File not found: " + fileNotFound.get().getAbsolutePath());
        }

        // replace directories in list by the json and yaml files in them
        var dirs = options.getFiles().stream().filter(File::isDirectory).collect(Collectors.toSet());
        var filesFromDirs = dirs.stream().flatMap(dir -> InputFileUtil.getJsonAndYamlFiles(dir).stream()).toList();
        var filesInRootFolder = InputFileUtil.getJsonAndYamlFiles(options.getFiles().stream().filter(File::isFile).toList());

        filesToProcess.addAll(filesInRootFolder);
        filesToProcess.addAll(filesFromDirs);
    }

    private static void printCommandLineArguments(String[] args) {
        log.info("Using: belgif-rest-guide-validator-{}", VersionProvider.getMavenVersion());
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

    private void initOutputProcessor() {
        initOutputTypes();
        if (options.getOutputTypes() == null || options.getOutputTypes().isEmpty())
            outputProcessors = Set.of(new ConsoleOutputProcessor(this.groupBy));
        else {
            try {
                Files.createDirectories(options.getOutputDir());
                Files.createDirectories(jsonOutputFile.getParentFile().toPath());
            } catch (IOException e) {
                log.error(options.getOutputDir() + " directory doesn't exist and cannot be created!", e);
            }

            outputProcessors = new HashSet<>();
            this.outputTypes.forEach(outputType -> {
                switch (outputType) {
                    case NONE:
                        break;
                    case JUNIT:
                        outputProcessors.add(new JUnitOutputProcessor(this.groupBy, options.getOutputDir().toFile()));
                        break;
                    case LOG4J:
                        outputProcessors.add(new Log4JOutputProcessor(this.groupBy));
                        break;
                    case JSON:
                        outputProcessors.add(new JsonOutputProcessor(this.groupBy, this.jsonOutputFile));
                        break;
                    default:
                        outputProcessors.add(new ConsoleOutputProcessor(this.groupBy));
                }
            });
        }
    }
}
