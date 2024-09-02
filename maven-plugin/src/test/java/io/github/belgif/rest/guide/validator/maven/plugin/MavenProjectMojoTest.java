package io.github.belgif.rest.guide.validator.maven.plugin;

import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MavenProjectMojoTest extends AbstractValidatorMojoTest {

    @Override
    protected MavenProjectMojo getMojo() {
        var openApiMojo = new MavenProjectMojo();
        openApiMojo.mavenProject = new MavenProject();
        openApiMojo.mavenProject.setFile(new File(this.getClass().getResource(".").getFile()));
        return openApiMojo;
    }

    @Test
    void executeSkipOnErrors() {
        var openApiMojo = getMojo();
        openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "swagger_bad.yaml")});
        openApiMojo.skipOnErrors = true;
        assertDoesNotThrow(openApiMojo::execute);
    }

}