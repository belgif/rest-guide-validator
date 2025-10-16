package org.openapi4j.core.model.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.Reference;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BelgifReferenceResolver extends AbstractBelgifReferenceResolver<OpenApiDefinition<?>> {

    public BelgifReferenceResolver(Map<File, JsonNode> documents, Parser.ParserResult result, ReferenceRegistry registry) throws IOException {
        super(documents, result, registry);
    }

    @Override
    public void resolve() throws IOException {
        for (OpenApiDefinition<?> definition : concernedDefinitions) {
            Reference<?> reference = (Reference<?>) definition.getModel();
            String refValue = reference.getRef();
            addRef(refValue, definition, result.resolve(definition.getModel()));
        }
    }

    @Override
    protected void addAbsoluteRefs(JsonNode baseDocument, File openApiFile) {
        concernedDefinitions.stream()
                .filter(def -> def.getOpenApiFile().equals(openApiFile))
                .forEach(def -> {
                    OpenApiDefinition<?> resolvedDefinition = result.resolve(def.getModel());
                    try {
                        String absoluteRef = resolvedDefinition.getOpenApiFile().getAbsoluteFile().toURI().toURL() + "#" + resolvedDefinition.getJsonPointer();
                        ObjectNode node = (ObjectNode) findJsonNode(def, baseDocument);
                        node.put("abs$ref", absoluteRef);
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    protected Set<OpenApiDefinition<?>> findConcernedDefinitions() {
        return result.getAllDefinitions().stream()
                .filter(def -> def.getModel() instanceof org.eclipse.microprofile.openapi.models.Reference && ((org.eclipse.microprofile.openapi.models.Reference<?>) def.getModel()).getRef() != null && !((Reference<?>) def.getModel()).getRef().isBlank())
                .collect(Collectors.toSet());
    }
}
