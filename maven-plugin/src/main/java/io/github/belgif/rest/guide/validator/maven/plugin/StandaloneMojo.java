package io.github.belgif.rest.guide.validator.maven.plugin;

import io.github.belgif.rest.guide.validator.ConsoleOutputProcessor;
import io.github.belgif.rest.guide.validator.OpenApiValidator;
import io.github.belgif.rest.guide.validator.OutputProcessor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Maven plugin that checks if a Swagger or an OpenAPI is conform to the Belgif REST guide standards.
 * The plugin use the following parameter:
 * - api-validator.files: files to validate
 * - api-validator.excludeResources: exclude paths that should not be validated. Comma seperated
 * Example:
 * mvn io.github.belgif.rest:rest-styleguide-validation-maven-plugin:<VERSION>:validate-openapi -Dapi-validator.files=C:/tmp/swagger.yaml -Dapi-validator.excludeResources=/persons/{socialSecurityNumber},/persons/createWithForm
 */
@Mojo(name = "validate-openapi", requiresProject = false)
public class StandaloneMojo extends AbstractMojo {
    static final String FAILURE_MESSAGE = "At least 1 error in validation !";

    @Parameter(property = "api-validator.files")
    List<File> files = new ArrayList<>();

    /**
     * @deprecated Please use x-ignore-rules in the OpenApi file or excludedFiles in the POM to exclude complete files.
     */
    @Deprecated(since = "1.2.2")
    @Parameter(property = "api-validator.excludeResources")
    List<String> excludeResources = new ArrayList<>();

    @Parameter(property = "api-validator.excludedFiles")
    List<String> excludedFiles = new ArrayList<>();
    private Set<OutputProcessor> outputProcessors;

    List<FileWithExclusion> filesToProcess = new ArrayList<>();

    private List<File> getJsonAndYamlFiles(File directory) {
        return getJsonAndYamlFiles(List.of(Objects.requireNonNull(directory.listFiles())));
    }

    private List<File> getJsonAndYamlFiles(List<File> fileList) {
        return fileList.stream().filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).collect(Collectors.toList());
    }

    private void init() throws FileNotFoundException {
        initOutputProcessor();
        addExclusions(initFiles());
    }

    private void initOutputProcessor() {
        outputProcessors = Set.of(new ConsoleOutputProcessor[]{new ConsoleOutputProcessor()});
    }

    private List<File> initFiles() throws FileNotFoundException {
        if (files.isEmpty())
            throw new IllegalArgumentException("api-validator need at least one file ! Set the 'api-validator.files' parameter.");
        Optional<File> fileNotFound = files.stream().filter(file -> !file.exists()).findAny();
        if (fileNotFound.isPresent()) {
            throw new FileNotFoundException("File not found: " + fileNotFound.get().getAbsolutePath());
        }

        // replace directories in list by the json and yaml files in them
        var dirs = files.stream().filter(File::isDirectory).collect(Collectors.toSet());
        var filesFromDirs = dirs.stream().flatMap(dir -> getJsonAndYamlFiles(dir).stream()).collect(Collectors.toList());
        var filesInRootFolder = files.stream().filter(File::isFile).filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).collect(Collectors.toList());
        List<File> fileList = new ArrayList<>();

        fileList.addAll(filesInRootFolder);
        fileList.addAll(filesFromDirs);
        return fileList;
    }

    private void addExclusions(List<File> fileList) {
        filesToProcess.forEach(mutableFileWithExclusion -> mutableFileWithExclusion.getExcludesPaths().addAll(excludeResources));
        filesToProcess.addAll(fileList.stream()
                .map(file -> new FileWithExclusion(file, excludeResources))
                .collect(Collectors.toList()));
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
        filesToProcess.forEach(fileWithExclusion -> {
            var file = fileWithExclusion.getFile();
            isValid.set(OpenApiValidator.isOasValid(file, fileWithExclusion.getExcludesPaths(), excludedFiles, outputProcessors.toArray(new OutputProcessor[0])) && isValid.get());
        });

        if (filesToProcess.isEmpty()) {
            isValid.set(false);
        }

        if (!isValid.get())
            throw new MojoFailureException(FAILURE_MESSAGE);
    }


}
