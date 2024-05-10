package io.github.belgif.rest.guide.validator.maven.plugin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileWithExclusion {
    private File file;
    private List<String> excludesPaths;
}
