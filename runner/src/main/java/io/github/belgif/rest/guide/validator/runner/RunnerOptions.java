package io.github.belgif.rest.guide.validator.runner;

import io.github.belgif.rest.guide.validator.runner.output.OutputGroupBy;
import io.github.belgif.rest.guide.validator.runner.output.OutputType;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Getter
@Builder
public class RunnerOptions {

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
}
