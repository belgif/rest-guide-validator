package io.github.belgif.rest.guide.validator.maven.plugin;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MavenProjectMojoTest extends AbstractValidatorMojoTest {

    @Override
    protected MavenProjectMojo getMojo() {
        return new MavenProjectMojo();
    }

    @Test
    void executeSkipOnErrors() {
        var openApiMojo = getMojo();
        openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "swagger_bad.yaml")});
        openApiMojo.skipOnErrors = true;
        assertDoesNotThrow(openApiMojo::execute);
    }

}