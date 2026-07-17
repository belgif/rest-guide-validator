package io.github.belgif.rest.guide.validator.core.parser;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.belgif.rest.guide.validator.core.util.JsonNodeUtil;
import lombok.Getter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Getter
public class SourceDefinition {
    private static final String REF_ONLY_KEY = "x-reusable-definitions-only";

    private final String fileName;
    private final File file;
    private final String src;
    private final boolean isYaml;
    private final OpenAPI openApi;
    private final boolean hasReusableDefinitionsOnly;
    private final JsonNode jsonNode;
    private final String version;

    public SourceDefinition(File file, OpenAPI openApi) throws IOException {
        this.file = file;
        this.fileName = file.getName();
        this.src = Files.readString(file.toPath());
        this.isYaml = checkIsYaml(this.fileName);
        this.openApi = openApi;
        this.hasReusableDefinitionsOnly = findHasReusableDefinitionsOnly(openApi);
        this.jsonNode = JsonNodeUtil.parseJsonNode(file, isYaml);
        this.version = findVersion(openApi, jsonNode, file);
    }

    public static boolean checkIsYaml(String fileName) {
        return fileName.endsWith("yaml") || fileName.endsWith("yml");
    }

    public boolean hasReusableDefinitionsOnly() {
        return hasReusableDefinitionsOnly;
    }

    private static boolean findHasReusableDefinitionsOnly(OpenAPI openApi) {
        if (openApi.getExtensions() != null &&
                openApi.getExtensions().containsKey(REF_ONLY_KEY) &&
                openApi.getExtensions().get(REF_ONLY_KEY) instanceof Boolean) {
            return ((Boolean) openApi.getExtensions().get(REF_ONLY_KEY));
        }
        return false;
    }

    private static String findVersion(OpenAPI openapi, JsonNode jsonNode, File file) {
        if (jsonNode.has("openapi")) {
            return openapi.getOpenapi();
        } else if (jsonNode.has("swagger")) {
            return jsonNode.get("swagger").asText();
        }
        throw new RuntimeException("Unable to find OAS version for " + file.getAbsolutePath());
    }

}
