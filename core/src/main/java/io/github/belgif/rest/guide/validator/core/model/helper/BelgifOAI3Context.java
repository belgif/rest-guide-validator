package io.github.belgif.rest.guide.validator.core.model.helper;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.OAIContext;
import org.openapi4j.core.model.reference.BelgifReferenceResolver;
import org.openapi4j.core.model.reference.DiscriminatorBelgifReferenceResolver;
import org.openapi4j.core.model.reference.ReferenceRegistry;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * This Context class is made in order to have a simple mapping between te already parsed context of the Belgif-Rest-Guide-Validator and the OpenApi4j framework.
 * openapi4j had some bugs in its parsing regarding the resolving of references. Using the belgif context prevents the openapi4j validators from crashing.
 */
public class BelgifOAI3Context implements OAIContext {

    private final ReferenceRegistry referenceRegistry;
    private final URL baseURL;
    private final JsonNode baseDocument;
    private final Map<File, JsonNode> documents;

    public BelgifOAI3Context(OpenApiDefinition<?> openApiDefinition) throws ResolutionException {
        try {
            this.baseURL = findBaseUrl(openApiDefinition);
            this.documents = new HashMap<>();
            this.referenceRegistry = buildReferenceRegistry(openApiDefinition, this.baseURL);
            this.baseDocument = findBaseDocument(openApiDefinition);
        } catch (IOException e) {
            throw new ResolutionException("Unable to parse base document", e);
        }
    }

    @Override
    public ReferenceRegistry getReferenceRegistry() {
        return referenceRegistry;
    }

    @Override
    public URL getBaseUrl() {
        return this.baseURL;
    }

    @Override
    public JsonNode getBaseDocument() {
        return baseDocument;
    }

    private URL findBaseUrl(OpenApiDefinition<?> openApiDefinition) throws MalformedURLException {
        return openApiDefinition.getOpenApiFile().toURI().toURL();
    }

    private JsonNode findBaseDocument(OpenApiDefinition<?> openApiDefinition) {
        return documents.get(openApiDefinition.getOpenApiFile().getAbsoluteFile());
    }

    private ReferenceRegistry buildReferenceRegistry(OpenApiDefinition<?> openApiDefinition, URL baseURL) throws IOException {
        ReferenceRegistry registry = new ReferenceRegistry(baseURL);
        Parser.ParserResult result = openApiDefinition.getResult();
        BelgifReferenceResolver referenceResolver = new BelgifReferenceResolver(documents, result, registry);
        referenceResolver.resolve();

        DiscriminatorBelgifReferenceResolver discriminatorResolver = new DiscriminatorBelgifReferenceResolver(documents, result, registry);
        discriminatorResolver.resolve();

        return registry;
    }
}
