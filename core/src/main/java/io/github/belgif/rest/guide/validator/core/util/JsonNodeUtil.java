package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;

import java.io.File;
import java.io.IOException;

public class JsonNodeUtil {

    private static ObjectMapper YAML_MAPPER;
    private static ObjectMapper JSON_MAPPER;

    public static JsonNode parseJsonNode(File file, boolean yaml) {
        try {
            return getCorrectMapper(yaml).readTree(file);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read openapi file <<" + file.getAbsolutePath() + ">> for JsonNode extraction", e);
        }
    }

    public static JsonNode findJsonNode(JsonPointer pointer, File openapiFile, Parser.ParserResult result) {
        return result.getSrc().get(openapiFile.getAbsolutePath()).getJsonNode().at(com.fasterxml.jackson.core.JsonPointer.compile(pointer.getJsonPointer()));
    }

    private static ObjectMapper getCorrectMapper(boolean yaml) {
        if (yaml) {
            if (YAML_MAPPER == null) {
                YAML_MAPPER = new ObjectMapper(new YAMLFactory());
            }
            return YAML_MAPPER;
        } else {
            if (JSON_MAPPER == null) {
                JSON_MAPPER = new ObjectMapper();
            }
            return JSON_MAPPER;
        }
    }

}
