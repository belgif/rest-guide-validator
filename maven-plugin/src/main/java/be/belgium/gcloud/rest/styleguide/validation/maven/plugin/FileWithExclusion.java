package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class FileWithExclusion {
    private String file;
    private List<String> excludesPaths;

}
