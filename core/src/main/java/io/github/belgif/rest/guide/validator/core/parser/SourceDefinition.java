package io.github.belgif.rest.guide.validator.core.parser;

import lombok.Getter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Getter
public class SourceDefinition {

    private final String fileName;
    private final File file;
    private final String src;
    private final boolean isYaml;
    private final OpenAPI openApi;
    private final boolean pathsUsedAsRefsOnly;

    public SourceDefinition(File file, OpenAPI openApi) throws IOException {
        this.file = file;
        this.fileName = file.getName();
        this.src = Files.readString(file.toPath());
        this.isYaml = checkIsYaml(this.fileName);
        this.openApi = openApi;
        this.pathsUsedAsRefsOnly = findPathsUsedAsRefsOnly(openApi);
    }

    public static boolean checkIsYaml(String fileName) {
        return fileName.endsWith("yaml") || fileName.endsWith("yml");
    }

    private static boolean findPathsUsedAsRefsOnly(OpenAPI openApi) {
        if (openApi.getExtensions() != null &&
                openApi.getExtensions().containsKey("x-paths-used-as-refs-only") &&
                openApi.getExtensions().get("x-paths-used-as-refs-only") instanceof Boolean) {
                return ((Boolean) openApi.getExtensions().get("x-paths-used-as-refs-only"));
            }
        return false;
    }
}
