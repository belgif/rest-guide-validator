package be.belgium.gcloud.rest.styleguide.validation.core.parser;

import lombok.Getter;

@Getter
public class SourceDefinition {

    private final String fileName;
    private final String src;
    private final boolean isYaml;

    public SourceDefinition(String fileName, String src, boolean isYaml) {
        this.fileName = fileName;
        this.src = src;
        this.isYaml = isYaml;
    }
}
