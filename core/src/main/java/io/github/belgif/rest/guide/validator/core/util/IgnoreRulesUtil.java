package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.parser.SourceDefinition;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreRulesUtil {

    private static final String X_IGNORE_RULES = "x-ignore-rules";

    private static ObjectMapper YAML_MAPPER;
    private static ObjectMapper JSON_MAPPER;

    private IgnoreRulesUtil() {
    }

    /*
    This method is necessary due to a bug in the swagger parser that ignores all extensions when a ref is found.
    https://github.com/swagger-api/swagger-parser/issues/2168

    This method only checks the definitions where a ref is present. The rest of the definitions already have the ignoredRules property up to date with the info from the swaggerparser.
     */
    public static void findIgnoreRules(Parser.ParserResult result) {
        Set<OpenApiDefinition<?>> definitionsWithRefs = result.getAllDefinitions()
                .stream().filter(OpenApiDefinition::hasReference).collect(Collectors.toSet());
        for (OpenApiDefinition<?> def : definitionsWithRefs) {
            addIgnoreRules(def);
        }
    }

    private static void addIgnoreRules(OpenApiDefinition<?> definition) {
        JsonNode node = getJsonNode(definition);
        if (node.has(X_IGNORE_RULES)) {
            JsonNode ignoredRules = node.get(X_IGNORE_RULES);
            Iterator<String> iterator = ignoredRules.fieldNames();
            while (iterator.hasNext()) {
                String ruleName = iterator.next();
                String reason = ignoredRules.get(ruleName).asText();
                definition.getIgnoredRules().put(ruleName, reason);
            }
        }
    }

    private static JsonNode getJsonNode(OpenApiDefinition<?> definition) {
        JsonNode openapi = getJsonNodeForSrc(getSrc(definition));
        return openapi.at(com.fasterxml.jackson.core.JsonPointer.compile(definition.getJsonPointer().getJsonPointer()));
    }

    private static JsonNode getJsonNodeForSrc(SourceDefinition src) {
        try {
            return getCorrectMapper(src).readTree(src.getSrc());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper getCorrectMapper(SourceDefinition sourceDefinition) {
        if (sourceDefinition.isYaml()) {
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

    private static SourceDefinition getSrc(OpenApiDefinition<?> definition) {
        return definition.getResult().getSrc().get(definition.getOpenApiFile().getAbsolutePath());
    }

}
