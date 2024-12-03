package io.github.belgif.rest.guide.validator.maven.plugin;

import io.github.belgif.rest.guide.validator.runner.ValidationRunner;
import io.github.belgif.rest.guide.validator.runner.output.OutputType;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractValidatorMojo extends AbstractMojo {
    protected static final String FAILURE_MESSAGE = "At least 1 error in validation !";
    private static final String DEFAULT_FILE_NAME = "validationReport.json";

    private Logger log = LoggerFactory.getLogger(AbstractValidatorMojo.class);

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

    @Parameter(property = "rest-guide-validator.failOnMissingOpenAPI", defaultValue = "true")
    protected boolean failOnMissingOpenAPI = true;

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
        if (Objects.equals(jsonOutputFile, new File(DEFAULT_FILE_NAME).getAbsoluteFile())) {
            jsonOutputFile = new File(String.valueOf(outputDir), DEFAULT_FILE_NAME);
        }
        ValidationRunner runner = ValidationRunner.builder()
                .files(files)
                .excludedFiles(excludedFiles)
                .jsonOutputFile(jsonOutputFile)
                .outputDir(outputDir.toPath())
                .outputTypes(outputTypes)
                .groupBy(groupBy)
                .build();
        try {
            return runner.executeRules();
        } catch (FileNotFoundException e) {
            if (failOnMissingOpenAPI) {
                throw new MojoFailureException(e.getMessage());
            } else {
                log.info(e.getMessage());
                return true;
            }
        }
    }

}
