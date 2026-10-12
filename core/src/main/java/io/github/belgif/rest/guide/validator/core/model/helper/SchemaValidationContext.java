package io.github.belgif.rest.guide.validator.core.model.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.dialect.BasicDialectRegistry;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.keyword.KeywordType;
import com.networknt.schema.keyword.NonValidationKeyword;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;

import java.util.HashMap;
import java.util.Map;

public final class SchemaValidationContext {
    private SchemaValidationContext() {
    }

    public static SchemaLocation location(OpenApiDefinition<?> definition) {
        return SchemaLocation.of(definition.getOpenApiFile().toURI() + "#" + definition.getJsonPointer());
    }

    public static SchemaRegistry createRegistry(SchemaDefinition definition) {
        var result = definition.getResult();
        Map<String, JsonNode> documents = new HashMap<>();
        result.getSrc().values().forEach(source ->
                documents.put(source.getFile().toURI().toString(), source.getJsonNode().deepCopy()));

        // Resolve through the parser, including file refs with a trailing slash and bare discriminator mappings.
        for (OpenApiDefinition<?> def : result.getAllDefinitions()) {
            if (def.hasReference()) {
                ObjectNode node = (ObjectNode) documents.get(def.getOpenApiFile().toURI().toString())
                        .at(def.getJsonPointer().getJsonPointer());
                node.put("$ref", location(result.resolve(def.getModel())).toString());
            }
        }
        for (SchemaDefinition schema : result.getSchemas()) {
            var discriminator = schema.getModel().getDiscriminator();
            if (discriminator != null && discriminator.getMapping() != null) {
                ObjectNode mapping = (ObjectNode) documents.get(schema.getOpenApiFile().toURI().toString())
                        .at(schema.getJsonPointer().getJsonPointer()).get("discriminator").get("mapping");
                discriminator.getMapping().forEach((key, value) -> mapping.put(key,
                        location(result.resolveDiscriminatorMapping(schema, value).orElseThrow()).toString()));
            }
        }

        var openApi30 = Dialects.getOpenApi30();
        var dialect = Dialect.builder(openApi30.getId(), openApi30)
                .keyword(new NonValidationKeyword("discriminator"))
                .keyword(new DiscriminatorCompositionKeyword(KeywordType.ALL_OF, documents))
                .keyword(new DiscriminatorCompositionKeyword(KeywordType.ONE_OF, documents))
                .keyword(new DiscriminatorCompositionKeyword(KeywordType.ANY_OF, documents))
                .build();
        Map<String, String> schemas = new HashMap<>();
        documents.forEach((uri, node) -> schemas.put(uri, node.toString()));
        return SchemaRegistry.withDialect(openApi30, builder -> builder
                .dialectRegistry(new BasicDialectRegistry(dialect))
                .schemaRegistryConfig(SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build())
                .schemas(schemas));
    }
}
