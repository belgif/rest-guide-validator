package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.cli.options.ValidatorOptions;
import io.github.belgif.rest.guide.validator.output.*;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@CommandLine.Command(name = "belgif-validate-openapi", mixinStandardHelpOptions = true, showDefaultValues = true)
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
        System.out.println("Belgif Rest Guide Cli started");

        System.out.println(options.getFiles());
        System.out.println(options.getExcludedFiles());
        System.out.println(options.getOutputTypes());
        System.out.println(options.getOutputDir());
        System.out.println(options.getJsonOutputFile());
        System.out.println(options.getGroupBy());
        try {
            init();
            myTempMethod();
        } catch (Exception e) {
            log.error(e.getMessage());
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new BelgifRestGuideCli()).execute(args);
        System.exit(exitCode);
    }

    private void myTempMethod() {
        System.out.println("Starting validation");
        var violationReports = filesToProcess.stream().map(file -> OpenApiValidator.callRules(file, options.getExcludedFiles())).toList();
        System.out.println(violationReports.size() + " violationReports");
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
        var filesFromDirs = dirs.stream().flatMap(dir -> getJsonAndYamlFiles(dir).stream()).toList();
        var filesInRootFolder = getJsonAndYamlFiles(options.getFiles().stream().filter(File::isFile).toList());

        filesToProcess.addAll(filesInRootFolder);
        filesToProcess.addAll(filesFromDirs);
    }

    //TODO can be reused from the maven project.
    private List<File> getJsonAndYamlFiles(File directory) {
        return getJsonAndYamlFiles(List.of(Objects.requireNonNull(directory.listFiles())));
    }

    private List<File> getJsonAndYamlFiles(List<File> fileList) {
        return fileList.stream().filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).toList();
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
