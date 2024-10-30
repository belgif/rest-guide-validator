package io.github.belgif.rest.guide.validator.runner;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.runner.output.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ValidationRunner {

    private ValidationRunner() {
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
    public static Set<OutputProcessor> buildOutputProcessors(List<OutputType> outputTypes, OutputGroupBy groupBy, Path outputPath, File jsonOutputFile) {
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
                            log.error(outputPath + " directory doesn't exist and cannot be created!", e);
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
                            log.error(jsonOutputFile.getParentFile().toPath() + " directory doesn't exist and cannot be created!", e);
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
}
