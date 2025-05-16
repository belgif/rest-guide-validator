package org.openapi4j.core.model.v3;

import com.fasterxml.jackson.databind.JsonNode;
import org.openapi4j.core.model.AuthOption;
import org.openapi4j.core.model.reference.AbstractReferenceResolver;
import org.openapi4j.core.model.reference.ReferenceRegistry;

import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.$REF;
import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.MAPPING;

/**
 * The JSON reference resolver for discriminator mapping.
 */
class MappingReferenceResolver extends AbstractReferenceResolver {
    MappingReferenceResolver(URL baseUrl, List<AuthOption> authOptions, JsonNode apiNode, ReferenceRegistry referenceRegistry) {
        super(baseUrl, authOptions, apiNode, $REF, referenceRegistry);
    }

    /*
     * Belgif modifications in order to fix a bug where references by schema name threw exceptions
     * The original class file can be found here: https://github.com/openapi4j/openapi4j/blob/master/openapi-core/src/main/java/org/openapi4j/core/model/v3/MappingReferenceResolver.java
     */
    @Override
    protected Collection<JsonNode> getReferencePaths(JsonNode document) {
        Collection<JsonNode> referenceNodes = document.findValues(MAPPING);

        Collection<JsonNode> referencePaths = new HashSet<>();

        List<JsonNode> refParents = document.findParents($REF);

        for (JsonNode refNode : referenceNodes) {
            for (JsonNode mappingNode : refNode) {
                if (mappingNode.textValue().contains("#/")) {
                    referencePaths.add(mappingNode);
                } else {
                    Optional<JsonNode> ref = refParents.stream().filter(parentNode ->
                                    parentNode.get($REF).textValue().endsWith(mappingNode.textValue()))
                            .findFirst();
                    if (ref.isPresent()) {
                        referencePaths.add(ref.get().get($REF));
                    } else {
                        referencePaths.add(mappingNode);
                    }
                }
            }
        }

        return referencePaths;
    }

}