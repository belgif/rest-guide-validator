package io.github.belgif.rest.guide.validator.maven.plugin;

import io.github.belgif.rest.guide.validator.runner.RunnerOptions;
import io.github.belgif.rest.guide.validator.runner.ValidationRunner;
import io.github.belgif.rest.guide.validator.runner.output.OutputType;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    Path outputDir;

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

    protected boolean executeRules() throws MojoFailureException {
        RunnerOptions options = RunnerOptions.builder()
                .files(files)
                .excludedFiles(excludedFiles)
                .jsonOutputFile(jsonOutputFile)
                .outputDir(outputDir)
                .outputTypes(outputTypes)
                .groupBy(groupBy)
                .build();
        try {
            return ValidationRunner.executeRules(options);
        } catch (FileNotFoundException e) {
            throw new MojoFailureException(e.getMessage());
        }
    }

}
