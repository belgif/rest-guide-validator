package io.github.belgif.rest.guide.validator.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Maven plugin that checks if an OpenAPI is conform to the Belgif REST guide standards.
 * Example:
 * mvn io.github.belgif.rest.guide.validator:belgif-rest-guide-validator-maven-plugin:[version]:validate-openapi "-Drest-guide-validator.files=openapi.yaml"
 */
@Mojo(name = "validate-openapi", requiresProject = false)
@ThreadSafe
public class StandaloneMojo extends AbstractValidatorMojo {
    @Override
    public void execute() throws MojoFailureException {
        getLog().info("Validating following files:" + files);
        var isValid = executeRules();

        if (!isValid)
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
