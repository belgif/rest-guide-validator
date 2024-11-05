package io.github.belgif.rest.guide.validator.cli.options;

import io.github.belgif.rest.guide.validator.cli.util.VersionProvider;
import lombok.Getter;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(synopsisHeading = "%nUsage:%n%n",
        descriptionHeading = "%nDescription:%n%n",
        parameterListHeading = "%nParameters:%n%n",
        optionListHeading = "%nOptions:%n%n",
        commandListHeading = "%nCommands:%n%n",
        versionProvider = VersionProvider.class)
@Getter
public class ValidatorOptions {

    @CommandLine.Parameters(paramLabel = "files", defaultValue = "openapi.yaml", description = "Files or folders. For a folder all json and yaml files will be used.")
    private List<File> files;

    @CommandLine.Option(names = {"-e", "--excludedFiles"}, description = "File(s) or folder(s) to exclude from validation. Use of wildcards is possible.")
    private List<String> excludedFiles;

    @CommandLine.Option(names = {"-t", "--outputTypes"}, defaultValue = "console", description = "Output processors. The value can be: CONSOLE, JUNIT, JSON, LOG4J, NONE")
    private List<String> outputTypes;

    @CommandLine.Option(names = {"-o", "--outputDir"}, defaultValue = "${DEFAULT-VALUE}", description = "Output directory for the validation report file (when outputType writes to a file)")
    private Path outputDir = Paths.get("").toAbsolutePath();

    @CommandLine.Option(names = {"-j", "--jsonOutputFile"}, defaultValue = "{outputDir}/validationReport.json", description = "Output file for JSON validation report.")
    private File jsonOutputFile = new File(outputDir.toFile(), "validationReport.json");

    @CommandLine.Option(names = {"-g", "--groupBy"}, defaultValue = "rule", description = "Specify how you want to group the violation output")
    private String groupBy;

    public List<String> getExcludedFiles() {
        return excludedFiles != null ? excludedFiles : new ArrayList<>();
    }

    public File getJsonOutputFile() {
        if (jsonOutputFile.getParentFile().toString().equals("{outputDir}")) {
            return new File(outputDir.toFile(), "validationReport.json");
        }
        if (jsonOutputFile.isAbsolute()) {
            return this.jsonOutputFile;
        } else {
            return new File(outputDir.toFile(), jsonOutputFile.getPath());
        }
    }
}
