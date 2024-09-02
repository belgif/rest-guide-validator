package io.github.belgif.rest.guide.validator.maven.plugin;

import io.github.belgif.rest.guide.validator.*;
import io.github.belgif.rest.guide.validator.output.ConsoleOutputProcessor;
import io.github.belgif.rest.guide.validator.output.Log4JOutputProcessor;
import io.github.belgif.rest.guide.validator.output.OutputProcessor;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maven plugin that checks if a Swagger or an OpenAPI is conform to the Belgif REST guide standards.
 * The plugin use the following parameters:
 * - api-validator.files: a list of files to validate
 * - api-validator.outputType: the output processor to process the violation. @see OutputType. Default is Console.
 * - api-validator.outputDir: the directory to write the XML Junit files. Only relevant for the OutputType.JUNIT
 * - api-validator.excludedFiles: Files that should not be validated.
 * - ${project} root directory for api-validator.files
 */
@Mojo(name = "api-validator", defaultPhase = LifecyclePhase.TEST_COMPILE)
public class MavenProjectMojo extends AbstractValidatorMojo {

    @Parameter(property = "api-validator.skipOnErrors")
    boolean skipOnErrors = false;

    @Parameter(property = "api-validator.outputTypes")
    List<OutputType> outputTypes;

    @Parameter(property = "api-validator.outputDir", defaultValue = "target")
    File outputDir;

    @Parameter(readonly = true, defaultValue = "${project}")
    MavenProject mavenProject;

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
            } catch (IOException e) {
                getLog().error(outputDir + " directory doesn't exist and cannot be created!", e);
            }

            outputProcessors = new HashSet<>();
            outputTypes.forEach(outputType -> {
                switch (outputType) {
                    case NONE:
                        break;
                    case JUNIT:
                        outputProcessors.add(new JUnitOutputProcessor(outputGroupBy));
                        break;
                    case LOG4J:
                        outputProcessors.add(new Log4JOutputProcessor(outputGroupBy));
                        break;
                    default:
                        outputProcessors.add(new ConsoleOutputProcessor(outputGroupBy));
                }
            });
        }
    }

    /**
     * For each file in @files validate using OpenApiValidator.
     *
     * @throws MojoFailureException when file is not a valid open-api.
     */
    @Override
    public void execute() throws MojoFailureException {
        try {
            init();
        } catch (FileNotFoundException e) {
            throw new MojoFailureException(e.getMessage());
        }

        var isValid = new AtomicBoolean(true);
        filesToProcess.forEach(file -> {
            // build output file for the jUnitOutputProcessor
            outputProcessors.stream().filter(DirectoryOutputProcessor.class::isInstance)
                    .map(o -> (DirectoryOutputProcessor) o)
                    .forEach(processor -> {
                        processor.setOutput(outputDir);
                        ((OutputProcessor) processor).setOutputGroupBy(outputGroupBy);
                    });

            isValid.set(OpenApiValidator.isOasValid(file, excludedFiles, outputProcessors.toArray(new OutputProcessor[0])) && isValid.get());
        });
        if (filesToProcess.isEmpty()) {
            isValid.set(false);
        }

        if (!skipOnErrors && !isValid.get())
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
