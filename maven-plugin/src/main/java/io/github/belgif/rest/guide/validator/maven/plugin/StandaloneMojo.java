package io.github.belgif.rest.guide.validator.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maven plugin that checks if a Swagger or an OpenAPI is conform to the Belgif REST guide standards.
 * The plugin use the following parameter:
 * - rest-guide-validator.files: files to validate
 * - rest-guide-validator.excludedFiles: Files that should not be validated.
 * - rest-guide-validator.groupBy: rule or file
 * - rest-guide-validator.outputType: the output processor to process the violation. @see OutputType. Default is Console.
 * - rest-guide-validator.outputDir: the directory to write the report files. Only relevant for OutputType.JUNIT and OutputType.JSON
 * Example:
 * mvn io.github.belgif.rest.guide.validator:belgif-rest-guide-validator-maven-plugin:[version]:validate-openapi "-Drest-guide-validator.files=openapi.yaml"
 */
@Mojo(name = "validate-openapi", requiresProject = false)
public class StandaloneMojo extends AbstractValidatorMojo {
    @Override
    public void execute() throws MojoFailureException {
        getLog().info("Validating following files:" + files);
        var isValid = new AtomicBoolean(true);
        executeRules(isValid);

        if (!isValid.get())
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
