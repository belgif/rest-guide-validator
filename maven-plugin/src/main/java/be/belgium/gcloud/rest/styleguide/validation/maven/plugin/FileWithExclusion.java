package be.belgium.gcloud.rest.styleguide.validation.maven.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileWithExclusion {
    private String file;
    private List<String> excludesPaths;
}
