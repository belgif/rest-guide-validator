package io.github.belgif.rest.guide.validator.maven.plugin;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.input.InputFileUtil;
import io.github.belgif.rest.guide.validator.output.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class AbstractValidatorMojo extends AbstractMojo {
    protected static final String FAILURE_MESSAGE = "At least 1 error in validation !";
    private static final String DEFAULT_FILE_NAME = "validationReport.json";

    /**
     * A list of files to validate
     */
    @Parameter(property = "rest-guide-validator.files")
    protected List<File> files = new ArrayList<>();

    /**
     * Files that should not be validated.
     */
    @Parameter(property = "rest-guide-validator.excludedFiles")
    protected List<String> excludedFiles = new ArrayList<>();

    /**
     * Specify how you want to group the violation output
     */
    @Parameter(property = "rest-guide-validator.groupBy", defaultValue = "rule")
    protected String groupBy = "rule";

    /**
     * the output processor to process the violation. @see OutputType. Default is Console.
     */
    @Parameter(property = "rest-guide-validator.outputTypes")
    List<OutputType> outputTypes;

    /**
     * Output directory for the validation report file
     */
    @Parameter(property = "rest-guide-validator.outputDir", defaultValue = "${project.build.directory}")
    File outputDir;

    /**
     * Output file for JSON validation report. If absent, default filename will be placed in rest-guide-validator.outputDir
     */
    @Parameter(property = "rest-guide-validator.jsonOutputFile", defaultValue = DEFAULT_FILE_NAME)
    File jsonOutputFile;

    /**
     * @deprecated fileWithExclusions parameter is ignored, please use x-ignore-rules in the OpenApi file or excludedFiles in the POM to exclude complete files.
     */
    @Deprecated(since = "1.2.2", forRemoval = true)
    @Parameter(property = "rest-guide-validator.fileWithExclusions")
    List<String> fileWithExclusions = new ArrayList<>();

    /**
     * @deprecated excludeResources parameter is ignored, please use x-ignore-rules in the OpenApi file or excludedFiles in the POM to exclude complete files.
     */
    @Deprecated(since = "1.2.2", forRemoval = true)
    @Parameter(property = "rest-guide-validator.excludeResources")
    List<String> excludeResources = new ArrayList<>();


    protected Set<OutputProcessor> outputProcessors;
    protected final List<File> filesToProcess = new ArrayList<>();
    protected OutputGroupBy outputGroupBy;

    protected void init() throws FileNotFoundException {
        outputGroupBy = OutputGroupBy.fromString(groupBy);
        if (Objects.equals(jsonOutputFile, new File(DEFAULT_FILE_NAME).getAbsoluteFile())) {
            jsonOutputFile = new File(outputDir, DEFAULT_FILE_NAME);
        }
        initOutputProcessor();
        initFiles();
    }

    protected boolean executeRules() throws MojoFailureException {
        var isValid = new AtomicBoolean(true);
        try {
            init();
        } catch (FileNotFoundException e) {
            throw new MojoFailureException(e.getMessage());
        }

        var violationReports = filesToProcess.stream().map(file -> OpenApiValidator.callRules(file, excludedFiles)).toList();
        isValid.set(violationReports.stream().allMatch(ViolationReport::isOasValid));
        outputProcessors.forEach(processor -> processor.process(new ViolationReport(violationReports)));
        if (filesToProcess.isEmpty()) {
            isValid.set(false);
        }
        return isValid.get();
    }

    /**
     * Add a Console ConsoleOutputProcessor if outputTypes is empty.
     * Instances Processors regarding the outputTypes.
     */
    protected void initOutputProcessor() {
        if (outputTypes == null || outputTypes.isEmpty())
            outputProcessors = Set.of(new ConsoleOutputProcessor(outputGroupBy));
        else {
            try {
                Files.createDirectories(outputDir.toPath());
                Files.createDirectories(jsonOutputFile.getParentFile().toPath());
            } catch (IOException e) {
                getLog().error(outputDir + " directory doesn't exist and cannot be created!", e);
            }

            outputProcessors = new HashSet<>();
            outputTypes.forEach(outputType -> {
                switch (outputType) {
                    case NONE:
                        break;
                    case JUNIT:
                        outputProcessors.add(new JUnitOutputProcessor(outputGroupBy, outputDir));
                        break;
                    case LOG4J:
                        outputProcessors.add(new Log4JOutputProcessor(outputGroupBy));
                        break;
                    case JSON:
                        outputProcessors.add(new JsonOutputProcessor(outputGroupBy, jsonOutputFile));
                        break;
                    default:
                        outputProcessors.add(new ConsoleOutputProcessor(outputGroupBy));
                }
            });
        }
    }

    /**
     * Throw an IllegalArgumentException if no file is provided or if a file is not in the maven project.
     * Add all yaml or gson file from provided directories.
     */
    private void initFiles() throws FileNotFoundException {
        if (files.isEmpty())
            throw new IllegalArgumentException("rest-guide-validator needs at least one file ! Set the 'rest-guide-validator.files' parameter.");
        Optional<File> fileNotFound = files.stream().filter(file -> !file.exists()).findAny();
        if (fileNotFound.isPresent()) {
            throw new FileNotFoundException("File not found: " + fileNotFound.get().getAbsolutePath());
        }

        // replace directories in list by the json and yaml files in them
        var dirs = files.stream().filter(File::isDirectory).collect(Collectors.toSet());
        var filesFromDirs = dirs.stream().flatMap(dir -> InputFileUtil.getJsonAndYamlFiles(dir).stream()).toList();
        var filesInRootFolder = files.stream().filter(File::isFile).filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).toList();

        filesToProcess.addAll(filesInRootFolder);
        filesToProcess.addAll(filesFromDirs);
    }

}
