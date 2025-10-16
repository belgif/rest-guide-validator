package org.openapi4j.core.model.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.lang.module.ResolutionException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DiscriminatorBelgifReferenceResolver extends AbstractBelgifReferenceResolver<SchemaDefinition> {

    public DiscriminatorBelgifReferenceResolver(Map<File, JsonNode> documents, Parser.ParserResult result, ReferenceRegistry registry) throws IOException {
        super(documents, result, registry);
    }

    @Override
    public void resolve() throws IOException {
        for (SchemaDefinition definition : concernedDefinitions) {
            definition.getModel().getDiscriminator().getMapping().values().forEach(mapping -> {
                String ref = mapping.contains("#") ? mapping : "#/components/schemas/" + mapping;
                SchemaDefinition resolvedSchema = result.resolveDiscriminatorMapping(definition, mapping).orElseThrow(ResolutionException::new);
                try {
                    addRef(ref, definition, resolvedSchema);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    protected void addAbsoluteRefs(JsonNode baseDocument, File openApiFile) {
        Set<SchemaDefinition> discriminatorMappings = concernedDefinitions.stream()
                .filter(def -> def.getOpenApiFile().equals(openApiFile))
                .collect(Collectors.toSet());

        for (SchemaDefinition definition : discriminatorMappings) {
            definition.getModel().getDiscriminator().getMapping().forEach((key, value) -> {
                SchemaDefinition resolvedSchema = result.resolveDiscriminatorMapping(definition, value).orElseThrow(ResolutionException::new);
                JsonNode schemaNode = findJsonNode(definition, baseDocument);
                ObjectNode node = (ObjectNode) schemaNode.get("discriminator").get("mapping");
                try {
                    String absoluteRef = resolvedSchema.getOpenApiFile().getAbsoluteFile().toURI().toURL() + "#" + resolvedSchema.getJsonPointer();
                    node.put(key, absoluteRef);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    protected Set<SchemaDefinition> findConcernedDefinitions() {
        return result.getSchemas().stream()
                .filter(def -> def.getModel().getDiscriminator() != null && def.getModel().getDiscriminator().getMapping() != null)
                .collect(Collectors.toSet());
    }
}
