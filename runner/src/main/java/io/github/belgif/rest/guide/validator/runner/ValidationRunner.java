package io.github.belgif.rest.guide.validator.runner;

import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.runner.output.OutputProcessor;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
}
