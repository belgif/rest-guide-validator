package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import be.belgium.gcloud.rest.styleguide.validation.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Maven plugin that check if a Swagger API or an open API is conformed the G-Cloud standards.
 * The plugin use the following parameters:
 *  - api-validator.files: a list of files to validate
 *  - api-validator.outputType: the output processor to process the violation. @see OutputType. Default is Console.
 *  - api-validator.outputDir: the directory to write the XML Junit files. Only relevant for the OutputType.JUNIT
 *  - ${project} root directory for api-validator.files
 */
@Mojo(name = "api-validator", defaultPhase = LifecyclePhase.COMPILE)
public class OpenApiMojo extends AbstractMojo {

    static final String FAILURE_MESSAGE = "At least 1 error in validation !";

    @Parameter(property = "api-validator.files", required = true)
    List<String> files;

    @Parameter(property = "api-validator.outputType", required = false)
    OutputType outputType = OutputType.CONSOLE;

    @Parameter(property = "api-validator.outputDir", required = false, defaultValue = "target")
    String outputDir = "target";

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    private String base;
    private OutputProcessor outputProcessor;

    private void init(){
        if(files == null || files.isEmpty())
            throw new IllegalArgumentException("api-validator need at least one file! Add the 'api-validator.files' in the plugin configuration.");

        if(mavenProject != null)
            base = this.mavenProject.getBasedir().getAbsolutePath() + File.separator;
        else
            base = System.getProperty("user.dir") + File.separator;
        getLog().debug("Working directory: "+base);

        getLog().debug("Using outputProcessor: "+outputType.name());
        switch (outputType){
            case NONE: outputProcessor = null; break;
            case JUNIT: outputProcessor = new JUnitOutputProcessor();break;
            case LOG4J: outputProcessor = new Log4JOutputProcessor();break;
            default: outputProcessor = new ConsoleOutputProcessor();
        }
    }

    /**
     * For each file in @files validate using OpenApiValidator.
     * @throws MojoExecutionException when file cannot be read or parse.
     * @throws MojoFailureException when file is not a valid open-api.
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();

        AtomicBoolean isValid = new AtomicBoolean(true);

        files.stream()
                .map(filename -> new File(base+filename))
                .forEach(file->{
                    if(outputType == OutputType.JUNIT)
                        ((JUnitOutputProcessor) outputProcessor).setOutputFile(
                                new File(outputDir, "TEST-" + file.getName() + ".xml"));

                    isValid.set(OpenApiValidator.isOasValid(file, outputProcessor) && isValid.get());
                });

        if (! isValid.get())
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
