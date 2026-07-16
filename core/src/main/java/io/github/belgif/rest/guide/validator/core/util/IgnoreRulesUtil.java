package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;

import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreRulesUtil {

    private static final String X_IGNORE_RULES = "x-ignore-rules";

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
        JsonNode node = definition.getJsonNode();
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

}
