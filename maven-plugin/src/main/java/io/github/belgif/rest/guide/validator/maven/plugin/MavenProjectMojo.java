package io.github.belgif.rest.guide.validator.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Maven plugin that checks if a Swagger or an OpenAPI is conform to the Belgif REST guide standards.
 */

@Mojo(name = "validate", defaultPhase = LifecyclePhase.TEST_COMPILE)
@ThreadSafe
public class MavenProjectMojo extends AbstractValidatorMojo {

    /**
     * Parameter to avoid maven fail in case of validation error.
     */
    @Parameter(property = "rest-guide-validator.skipOnErrors")
    boolean skipOnErrors = false;

    /**
     * root directory for rest-guide-validator.files
     */
    @Parameter(readonly = true, defaultValue = "${project}")
    MavenProject mavenProject;

    /**
     * For each file in @files validate using OpenApiValidator.
     *
     * @throws MojoFailureException when file is not a valid open-api.
     */
    @Override
    public void execute() throws MojoFailureException {
        var isValid = executeRules();

        if (!skipOnErrors && !isValid)
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
