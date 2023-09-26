package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiMojoTest {
    private static final String BAS_DIR = "src/test/resources/be/belgium/gcloud/rest/styleguide/validation/rules/";

    private OpenApiMojo getMojo() {
        var openApiMojo = new OpenApiMojo();
        openApiMojo.mavenProject = new MavenProject();
        openApiMojo.mavenProject.setFile(new File(this.getClass().getResource(".").getFile()));
        return openApiMojo;
    }

    @Test
    void execute() {
        var openApiMojo = getMojo();
        openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "swagger_bad.yaml"), new File(BAS_DIR + "swagger4.yaml")});
        var exception = assertThrows(MojoFailureException.class, openApiMojo::execute);
        assertEquals(OpenApiMojo.FAILURE_MESSAGE, exception.getMessage());
        System.err.println("--Test-- sys.err visible in Jenkins test result ??");
    }

    @Test
    void executeNoFile() {
        var openApiMojo = getMojo();
        openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "notExist.yaml")});
        var exception = assertThrows(MojoFailureException.class, openApiMojo::execute);
        assertTrue(exception.getMessage().startsWith("File not found: "));
    }

    @Test
    void ExecuteSkipOnErrors() throws MojoFailureException {
        var openApiMojo = getMojo();
        openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "swagger4.yaml")});
        openApiMojo.skipOnErrors = true;

        openApiMojo.execute();
    }

    @Test
    void ExecuteWithExcludes() {
        var openApiMojo = getMojo();
        openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "swagger4.yaml")});
        openApiMojo.excludeResources = List.of(new String[]{"/api/doc/swagger.json", "/health", "/api/doc", "/api/healthCheck"});

        var exception = assertThrows(MojoFailureException.class, openApiMojo::execute);
        assertEquals(OpenApiMojo.FAILURE_MESSAGE, exception.getMessage());
    }
}