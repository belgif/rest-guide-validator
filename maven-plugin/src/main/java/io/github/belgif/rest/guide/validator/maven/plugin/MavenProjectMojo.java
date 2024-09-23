package io.github.belgif.rest.guide.validator.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maven plugin that checks if a Swagger or an OpenAPI is conform to the Belgif REST guide standards.
 * The plugin use the following parameters:
 * - rest-guide-validator.files: a list of files to validate
 * - rest-guide-validator.outputType: the output processor to process the violation. @see OutputType. Default is Console.
 * - rest-guide-validator.outputDir: the directory to write the report files. Only relevant for OutputType.JUNIT and OutputType.JSON
 * - rest-guide-validator.excludedFiles: Files that should not be validated.
 * - ${project} root directory for rest-guide-validator.files
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.TEST_COMPILE)
public class MavenProjectMojo extends AbstractValidatorMojo {

    @Parameter(property = "rest-guide-validator.skipOnErrors")
    boolean skipOnErrors = false;

    @Parameter(readonly = true, defaultValue = "${project}")
    MavenProject mavenProject;

    /**
     * For each file in @files validate using OpenApiValidator.
     *
     * @throws MojoFailureException when file is not a valid open-api.
     */
    @Override
    public void execute() throws MojoFailureException {
        var isValid = new AtomicBoolean(true);
        executeRules(isValid);

        if (!skipOnErrors && !isValid.get())
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
