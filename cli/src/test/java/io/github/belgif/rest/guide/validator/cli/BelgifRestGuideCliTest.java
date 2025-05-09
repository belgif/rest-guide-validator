package io.github.belgif.rest.guide.validator.cli;

import io.github.belgif.rest.guide.validator.cli.util.VersionProvider;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class BelgifRestGuideCliTest {

    @Test
    void testStatusCode1WhenNoArgumentsAreGiven() {
        BelgifRestGuideCli cli = new BelgifRestGuideCli();
        CommandLine cmd = new CommandLine(cli);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute();
        assertEquals(1, exitCode);
    }

    @Test
    void testHelp() {
        BelgifRestGuideCli cli = new BelgifRestGuideCli();
        CommandLine cmd = new CommandLine(cli);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute("--help");
        assertEquals(0, exitCode);
        assertTrue(sw.toString().contains("Options:"));
        assertFalse(sw.toString().contains("postInstall"));
    }

    @Test
    void testVersion() {
        BelgifRestGuideCli cli = new BelgifRestGuideCli();
        CommandLine cmd = new CommandLine(cli);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute("--version");
        assertEquals(0, exitCode);
        String version = VersionProvider.getValidatorVersion();
        assertTrue(sw.toString().contains(version));
    }

    @Test
    void testFailingOpenApiHasStatusCode11() throws URISyntaxException {
        var resourceUrl = getClass().getResource("failingOpenapi.yaml");
        Path path = Paths.get(resourceUrl.toURI());
        BelgifRestGuideCli cli = new BelgifRestGuideCli();
        CommandLine cmd = new CommandLine(cli);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute(path.toString());
        assertEquals(11, exitCode);
    }

    @Test
    void testPassingOpenApiHasStatusCode0() throws URISyntaxException {
        var resourceUrl = getClass().getResource("passingOpenapi.yaml");
        Path path = Paths.get(resourceUrl.toURI());
        BelgifRestGuideCli cli = new BelgifRestGuideCli();
        CommandLine cmd = new CommandLine(cli);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute(path.toString());
        assertEquals(0, exitCode);
    }

    @Test
    void testInvalidOutputTypeCode0() throws URISyntaxException {
        var resourceUrl = getClass().getResource("passingOpenapi.yaml");
        Path path = Paths.get(resourceUrl.toURI());
        BelgifRestGuideCli cli = new BelgifRestGuideCli();
        CommandLine cmd = new CommandLine(cli);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute(path.toString(), "--outputType=invalid");
        assertEquals(0, exitCode);
    }

    @Test
    void testLoggerOutputTypeNotSupportedInCliVersionCode0() throws URISyntaxException {
        var resourceUrl = getClass().getResource("passingOpenapi.yaml");
        Path path = Paths.get(resourceUrl.toURI());
        BelgifRestGuideCli cli = new BelgifRestGuideCli();
        CommandLine cmd = new CommandLine(cli);
        StringWriter sw = new StringWriter();
        cmd.setOut(new PrintWriter(sw));
        int exitCode = cmd.execute(path.toString(), "--outputType=logger");
        assertEquals(0, exitCode);
    }

}
