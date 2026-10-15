package org.openapi4j.core.model.reference;

import com.fasterxml.jackson.databind.JsonNode;
import org.openapi4j.core.model.OAIContext;

import java.net.URL;

public final class OpenApi4jTestContext {
    private OpenApi4jTestContext() {
    }

    public static OAIContext create(URL baseUrl, JsonNode document) {
        ReferenceRegistry references = new ReferenceRegistry(baseUrl);
        JsonNode schemas = document.at("/components/schemas");
        if (schemas.isObject()) {
            schemas.fields().forEachRemaining(entry -> {
                String pointer = "/components/schemas/" + entry.getKey().replace("~", "~0").replace("/", "~1");
                references.addRef(baseUrl, "#" + pointer).setContent(entry.getValue());
            });
        }
        return new OAIContext() {
            @Override
            public ReferenceRegistry getReferenceRegistry() {
                return references;
            }

            @Override
            public URL getBaseUrl() {
                return baseUrl;
            }

            @Override
            public JsonNode getBaseDocument() {
                return document;
            }
        };
    }
}
