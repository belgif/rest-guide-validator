package io.github.belgif.rest.guide.validator.runner;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.runner.input.InputFileUtil;
import io.github.belgif.rest.guide.validator.runner.output.*;
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
public class ValidationRunner {

    private ValidationRunner() {
    }

    public static boolean executeRules(RunnerOptions options) throws FileNotFoundException {
        Set<OutputProcessor> outputProcessors = buildOutputProcessors(options.getOutputTypes(), options.getOutputGroupBy(), options.getOutputDir(), options.getJsonOutputFile());
        List<File> filesToProcess = buildFilesToProcess(options.getFiles());
        return executeRules(filesToProcess, options.getExcludedFiles(), outputProcessors);
    }

    public static boolean executeRules(List<File> filesToProcess, List<String> excludedFiles, Set<OutputProcessor> outputProcessors) {
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

}
