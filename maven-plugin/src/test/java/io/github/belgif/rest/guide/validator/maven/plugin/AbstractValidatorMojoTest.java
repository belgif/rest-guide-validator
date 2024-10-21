package io.github.belgif.rest.guide.validator.maven.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.belgif.rest.guide.validator.output.OutputType;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

    @Test
    void testMultiThreaded() throws InterruptedException, IOException {
        ExecutorService executor = Executors.newFixedThreadPool(4);

        List<File> outputFiles = new ArrayList<>();

        List<Callable<Void>> tasks = getCallables(outputFiles);

        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        byte[] referenceBytes = Files.readAllBytes(outputFiles.get(0).getAbsoluteFile().toPath());
        // Assert results
        for (File outputFile : outputFiles) {
            assertTrue(outputFile.exists());
            byte[] bytes = Files.readAllBytes(outputFile.toPath());
            assertArrayEquals(referenceBytes, bytes);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(referenceBytes);
        assertTrue(node.has("violations"));
        assertEquals(4, node.get("totalViolations").asInt());
    }

    private List<Callable<Void>> getCallables(List<File> outputFiles) {
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < 48; i++) {
            tasks.add(() -> {
                var openApiMojo = getMojo();
                openApiMojo.files = List.of(new File[]{new File(BAS_DIR + "swagger_bad.yaml")});
                openApiMojo.groupBy = "rule";
                openApiMojo.outputDir = Paths.get("target").toFile();
                var tempFile = Files.createTempFile("tmpFile", "custom.json").toFile();
                outputFiles.add(tempFile);
                openApiMojo.jsonOutputFile = tempFile;
                openApiMojo.outputTypes = List.of(OutputType.JSON);
                openApiMojo.execute();
                return null;
            });
        }
        return tasks;
    }

}
