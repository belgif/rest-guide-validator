package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import be.belgium.gcloud.rest.styleguide.validation.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Maven plugin that checks if a Swagger or an OpenAPI is conform to the Belgif REST guide standards.
 * The plugin use the following parameters:
 *  - api-validator.files: a list of files to validate
 *  - api-validator.outputType: the output processor to process the violation. @see OutputType. Default is Console.
 *  - api-validator.outputDir: the directory to write the XML Junit files. Only relevant for the OutputType.JUNIT
 *  - ${project} root directory for api-validator.files
 */
@Mojo(name = "api-validator", defaultPhase = LifecyclePhase.TEST_COMPILE)
public class OpenApiMojo extends AbstractMojo {

    static final String FAILURE_MESSAGE = "At least 1 error in validation !";

    @Parameter(property = "api-validator.files")
    List<File> files = new ArrayList<>();

    @Parameter(property = "api-validator.fileWithExclusions")
    List<FileWithExclusion> fileWithExclusions = new ArrayList<>();

    @Parameter(property = "api-validator.skipOnErrors ")
    boolean skipOnErrors = false;

    @Parameter(property = "api-validator.outputTypes")
    List<OutputType> outputTypes ;

    @Parameter(property = "api-validator.outputDir", defaultValue = "target")
    File outputDir ;

    @Parameter(readonly = true, defaultValue = "${project}")
    MavenProject mavenProject;

    @Parameter(property = "api-validator.excludeResources")
    List<String> excludeResources = new ArrayList<>();

    private Set<OutputProcessor> outputProcessors;
    private final List<FileWithExclusion> filesToProcess = new ArrayList<>();

    private List<File> getJsonAndYamlFiles(File directory){
        return getJsonAndYamlFiles(List.of(Objects.requireNonNull(directory.listFiles())));
    }
    private List<File> getJsonAndYamlFiles(List<File> fileList) {
        return fileList.stream().filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).collect(Collectors.toList());
    }
    private void init() {
       initOutputProcessor();
       addExclusions(initFiles());
    }

    /**
     * Add a Console ConsoleOutputProcessor if outputTypes is empty.
     * Instances Processors regarding the outputTypes.
     */
    private void initOutputProcessor(){
        if ( outputTypes == null || outputTypes.isEmpty())
            outputProcessors = Set.of(new ConsoleOutputProcessor[]{new ConsoleOutputProcessor()});
        else {
            try {
                Files.createDirectories(outputDir.toPath());
            } catch (IOException e) {
                getLog().error(outputDir+" directory doesn't exist and cannot be created!", e);
            }

            outputProcessors = new HashSet<>();
            outputTypes.forEach(outputType -> {
                switch (outputType) {
                    case NONE: break;
                    case JUNIT:
                        outputProcessors.add(new JUnitOutputProcessor()); break;
                    case JUNIT2:
                        outputProcessors.add(new JUnitOutputProcessor2()); break;
                    case JUNIT3:
                        outputProcessors.add(new JUnitOutputProcessor3()); break;
                    case LOG4J:
                        outputProcessors.add(new Log4JOutputProcessor()); break;
                    default:
                        outputProcessors.add(new ConsoleOutputProcessor());
                }
            });
        }
    }

    /**
     * Throw an IllegalArgumentException if no file is provided or if a file is not in the maven project.
     * Add all yaml or gson file from provided directories.
     */
    private List<File> initFiles(){
        if( files.isEmpty() && fileWithExclusions.isEmpty() )
            throw new IllegalArgumentException("api-validator need at least one file ! Add the 'api-validator.files' or 'api-validator.fileWithExclusions' in the plugin configuration.");
        if( files.stream().anyMatch(file -> file.getPath().startsWith(mavenProject.getFile().getPath())) ||
            fileWithExclusions.stream().anyMatch(f -> f.getFile().getPath().startsWith(mavenProject.getFile().getPath())) )
            throw new IllegalArgumentException("All files must be in the maven project structure !");

        // replace directories in list by the json and yaml files in them
        var dirs = files.stream().filter(File::isDirectory).collect(Collectors.toSet());
        var filesFromDirs = dirs.stream().flatMap(dir -> getJsonAndYamlFiles(dir).stream()).collect(Collectors.toList());
        var filesInRootFolder = files.stream().filter(File::isFile).filter(file -> file.getName().endsWith(".yml") || file.getName().endsWith(".yaml") || file.getName().endsWith(".json")).collect(Collectors.toList());
        List<File> fileList = new ArrayList<>();

        fileList.addAll(filesInRootFolder);
        fileList.addAll(filesFromDirs);
        return fileList;
    }

    /**
     * add global exclusions to all files.
     * add all files in fileWithExclusions.
     */
    private void addExclusions(List<File> fileList){
        filesToProcess.addAll(fileWithExclusions);
        filesToProcess.forEach(mutableFileWithExclusion -> mutableFileWithExclusion.getExcludesPaths().addAll(excludeResources));
        filesToProcess.addAll( fileList.stream()
                .map(file -> new FileWithExclusion(file, excludeResources))
                .collect(Collectors.toList()) );
    }
    /**
     * For each file in @files validate using OpenApiValidator.
     * @throws MojoFailureException when file is not a valid open-api.
     */
    @Override
    public void execute() throws MojoFailureException {
        init();

        var isValid = new AtomicBoolean(true);
        filesToProcess.forEach(fileWithExclusion->{
                    var file = fileWithExclusion.getFile() ;
                    // build output file for the jUnitOutputProcessor
                    outputProcessors.stream().filter(outputProcessor -> outputProcessor instanceof JUnitOutputProcessor)
                            .map(o -> (JUnitOutputProcessor)o)
                            .forEach(jUnitOutputProcessor -> jUnitOutputProcessor.setOutputFile(
                                    new File(outputDir, "TEST-" + file.getName() + ".xml")));

                    outputProcessors.stream().filter(outputProcessor -> outputProcessor instanceof DirectoryOutputProcessor)
                            .map(o -> (DirectoryOutputProcessor)o)
                            .forEach(processor -> processor.setOutput(outputDir));

                    // isValid = isValid && OpenApiValidator.isOasValid(...)
                    isValid.set(OpenApiValidator.isOasValid(file, fileWithExclusion.getExcludesPaths(), outputProcessors.toArray(new OutputProcessor[0])) && isValid.get());
                });
        if ( filesToProcess.isEmpty() ) {
            isValid.set(false);
        }

        if (! skipOnErrors && ! isValid.get())
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
