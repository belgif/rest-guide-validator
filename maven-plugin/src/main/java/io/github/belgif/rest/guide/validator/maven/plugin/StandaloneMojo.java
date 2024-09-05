package io.github.belgif.rest.guide.validator.maven.plugin;

import io.github.belgif.rest.guide.validator.output.ConsoleOutputProcessor;
import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.output.OutputProcessor;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import java.io.FileNotFoundException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maven plugin that checks if a Swagger or an OpenAPI is conform to the Belgif REST guide standards.
 * The plugin use the following parameter:
 * - rest-guide-validator.files: files to validate
 * Example:
 * mvn io.github.belgif.rest:rest-styleguide-validation-maven-plugin:<VERSION>:validate-openapi -Drest-guide-validator.files=C:/tmp/swagger.yaml
 */
@Mojo(name = "validate-openapi", requiresProject = false)
public class StandaloneMojo extends AbstractValidatorMojo {
    @Override
    protected void initOutputProcessor() {
        outputProcessors = Set.of(new ConsoleOutputProcessor(outputGroupBy));
    }

    @Override
    public void execute() throws MojoFailureException {
        getLog().info("Validating following files:" + files);
        try {
            init();
        } catch (FileNotFoundException e) {
            throw new MojoFailureException(e.getMessage());
        }

        var isValid = new AtomicBoolean(true);
        filesToProcess.forEach(file -> isValid.set(OpenApiValidator.isOasValid(file, excludedFiles, outputProcessors.toArray(new OutputProcessor[0])) && isValid.get()));

        if (filesToProcess.isEmpty()) {
            isValid.set(false);
        }

        if (!isValid.get())
            throw new MojoFailureException(FAILURE_MESSAGE);
    }


}
