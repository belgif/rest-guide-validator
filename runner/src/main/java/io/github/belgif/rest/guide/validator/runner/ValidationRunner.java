package io.github.belgif.rest.guide.validator.runner;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.runner.input.InputFileUtil;
import io.github.belgif.rest.guide.validator.runner.output.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Builder
public class ValidationRunner {

    /**
     * A list of files to validate
     */
    @NonNull
    private final List<File> files;

    /**
     * Files that should not be validated.
     */
    private final List<String> excludedFiles;

    private final List<OutputType> outputTypes;
    private final String groupBy;
    private final Path outputDir;
    private final File jsonOutputFile;

    public OutputGroupBy getOutputGroupBy() {
        return OutputGroupBy.fromString(groupBy);
    }

    public boolean executeRules() throws FileNotFoundException {
        Set<OutputProcessor> outputProcessors = buildOutputProcessors(this.outputTypes, this.getOutputGroupBy(), this.outputDir, this.jsonOutputFile);
        List<File> filesToProcess = buildFilesToProcess(this.files);
        return executeRules(filesToProcess, this.excludedFiles, outputProcessors);
    }

    private static boolean executeRules(List<File> filesToProcess, List<String> excludedFiles, Set<OutputProcessor> outputProcessors) {
        var isValid = new AtomicBoolean(true);
        var violationReports = filesToProcess.stream().map(file -> OpenApiValidator.callRules(file, excludedFiles)).toList();
        isValid.set(violationReports.stream().allMatch(ViolationReport::isOasValid));
        outputProcessors.forEach(processor -> processor.process(new ViolationReport(violationReports)));
        return isValid.get();
    }

    /**
     * Add a Console ConsoleOutputProcessor if outputTypes is empty.
     * Instances Processors regarding the outputTypes.
     */
    private static Set<OutputProcessor> buildOutputProcessors(List<OutputType> outputTypes, OutputGroupBy groupBy, Path outputPath, File jsonOutputFile) {
        Set<OutputProcessor> outputProcessors;
        if (outputTypes == null || outputTypes.isEmpty())
            outputProcessors = Set.of(new ConsoleOutputProcessor(groupBy));
        else {
            outputProcessors = new HashSet<>();
            outputTypes.forEach(outputType -> {
                switch (outputType) {
                    case NONE:
                        break;
                    case JUNIT:
                        try {
                            Files.createDirectories(outputPath);
                        } catch (IOException e) {
                            log.error("{} directory doesn't exist and cannot be created!", outputPath, e);
                        }
                        outputProcessors.add(new JUnitOutputProcessor(groupBy, outputPath.toFile()));
                        break;
                    case LOG4J:
                        outputProcessors.add(new Log4JOutputProcessor(groupBy));
                        break;
                    case JSON:
                        try {
                            Files.createDirectories(jsonOutputFile.getParentFile().toPath());
                        } catch (IOException e) {
                            log.error("{} directory doesn't exist and cannot be created!", jsonOutputFile.getParentFile().toPath(), e);
                        }
                        outputProcessors.add(new JsonOutputProcessor(groupBy, jsonOutputFile));
                        break;
                    default:
                        outputProcessors.add(new ConsoleOutputProcessor(groupBy));
                }
            });
        }
        return outputProcessors;
    }

    private static List<File> buildFilesToProcess(List<File> files) throws FileNotFoundException {
        if (files == null || files.isEmpty())
            throw new IllegalArgumentException("rest-guide-validator needs at least one file ! Set the '-f' or '--files' parameter.");
        Optional<File> fileNotFound = files.stream().filter(file -> !file.exists()).findAny();
        if (fileNotFound.isPresent()) {
            throw new FileNotFoundException("File not found: " + fileNotFound.get().getAbsolutePath());
        }

        // replace directories in list by the json and yaml files in them
        var dirs = files.stream().filter(File::isDirectory).collect(Collectors.toSet());
        var filesFromDirs = dirs.stream().flatMap(dir -> InputFileUtil.getJsonAndYamlFiles(dir).stream()).toList();
        var filesInRootFolder = InputFileUtil.getJsonAndYamlFiles(files.stream().filter(File::isFile).toList());

        List<File> filesToProcess = new ArrayList<>();
        filesToProcess.addAll(filesInRootFolder);
        filesToProcess.addAll(filesFromDirs);

        return filesToProcess;
    }

    public String listOptions() {
        StringBuilder sb = new StringBuilder();
        sb.append("Using the following options:\n");
        if (files.size() == 1) {
            sb.append("\t").append("File: ").append(files.get(0).getAbsolutePath()).append("\n");
        } else {
            sb.append("\t").append("Files:\n");
            for (File file : files) {
                sb.append("\t\t").append(file.getAbsolutePath()).append("\n");
            }
        }
        if (excludedFiles != null && !excludedFiles.isEmpty()) {
            sb.append("\t").append("Excluded Files: ");
            for (String excludedFile : excludedFiles) {
                sb.append("\t\t").append(excludedFile).append("\n");
            }
        }
        if (outputTypes.size() == 1) {
            sb.append("\t").append("Output Type: ").append(outputTypes.get(0)).append("\n");
        } else {
            sb.append("\t").append("Output Types:\n");
            for (OutputType outputType : outputTypes) {
                sb.append("\t\t").append(outputType).append("\n");
            }
        }
            sb.append("\t").append("GroupBy: ").append(groupBy).append("\n");
        if (outputTypes.size() > 1 || (!outputTypes.contains(OutputType.CONSOLE) && !outputTypes.contains(OutputType.NONE))) {
            sb.append("\t").append("OutputDir: ").append(outputDir).append("\n");
        }
        if (outputTypes.contains(OutputType.JSON)) {
            sb.append("\t").append("JSON Output File: ").append(jsonOutputFile).append("\n");
        }
        return sb.toString();
    }

}
