package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import be.belgium.gcloud.rest.styleguide.validation.OpenApiValidator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Mojo(name = "api-validator", defaultPhase = LifecyclePhase.COMPILE)
public class OpenApiMojo extends AbstractMojo {

    static final String FAILURE_MESSAGE = "At least 1 error in validation !";

    @Parameter(property = "api-validator.files")
    List<String> files;

    @Parameter(readonly = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String base;
        if(mavenProject != null)
            base = this.mavenProject.getBasedir().getAbsolutePath() + File.separator;
        else
            base = System.getProperty("user.dir") + File.separator;
        getLog().debug("Working directory: "+base);

        var isValid = true;
        for (String filename: files){
            var file = new File(base+filename);
            try {
                isValid &= OpenApiValidator.isOasValid(file);
            }catch (IOException ex){
                throw new MojoExecutionException(ex);
            }
        }
        if ( ! isValid)
            throw new MojoFailureException(FAILURE_MESSAGE);
    }
}
