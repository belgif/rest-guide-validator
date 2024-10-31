package io.github.belgif.rest.guide.validator.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractValidatorMojoTest {
    protected static final String BAS_DIR = "src/test/resources/io/github/belgif/rest/guide/validator/rules/";

    protected abstract AbstractValidatorMojo getMojo();

    @Test
    void execute() {
        var openApiMojo = getMojo();
        openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "openapi.yaml"), new File(BAS_DIR + "swagger_bad.yaml")});
        var exception = assertThrows(MojoFailureException.class, openApiMojo::execute);
        assertEquals(MavenProjectMojo.FAILURE_MESSAGE, exception.getMessage());
    }

    @Test
    void executeNoFile() {
        var openApiMojo = getMojo();
        openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "notExist.yaml")});
        var exception = assertThrows(MojoFailureException.class, openApiMojo::execute);
        assertTrue(exception.getMessage().startsWith("File not found: "));
    }

}