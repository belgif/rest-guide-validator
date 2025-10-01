package org.openapi4j.core.model.reference;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.parser.SourceDefinition;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Set;

public abstract class AbstractBelgifReferenceResolver<T extends OpenApiDefinition<?>> {

    protected final Map<File, JsonNode> documents;
    protected final Parser.ParserResult result;
    protected final ReferenceRegistry registry;
    protected final Set<T> concernedDefinitions;

    protected AbstractBelgifReferenceResolver(Map<File, JsonNode> documents, Parser.ParserResult result, ReferenceRegistry registry) throws IOException {
        this.documents = documents;
        this.result = result;
        this.registry = registry;
        this.concernedDefinitions = findConcernedDefinitions();
        initializeJsonDocuments();
    }

    public abstract void resolve() throws IOException;

    protected void addRef(String refValue, OpenApiDefinition<?> openApiDefinition, OpenApiDefinition<?> resolvedDefinition) throws IOException {
        /*
        url should be a URL to the file where the reference is pointing, registryInputValue is the jsonpointer within the target file.
         */
        int hashIndex = refValue.indexOf('#');
        String registryInputValue = hashIndex > 0 ? refValue.substring(hashIndex) : refValue;

        URL url = hashIndex == 0 ? openApiDefinition.getOpenApiFile().toURI().toURL() : resolvedDefinition.getOpenApiFile().toURI().toURL();

        org.openapi4j.core.model.reference.Reference openApi4jRef = registry.addRef(url, registryInputValue);

        /*
        In each reference in the referenceRegistry the target JsonNode is placed. This way the openapi4j schemavalidator can do its work.
         */
        openApi4jRef.setContent(findJsonNode(resolvedDefinition, documents.get(resolvedDefinition.getOpenApiFile())));
    }

    protected abstract void addAbsoluteRefs(JsonNode baseDocument, File openApiFile) throws IOException;

    /*
    finds all definitions that should be checked (for instance, all discriminators or all References)
     */
    protected abstract Set<T> findConcernedDefinitions();

    protected JsonNode findJsonNode(OpenApiDefinition<?> openApiDefinition, JsonNode baseDocument) {
        return baseDocument.at(JsonPointer.compile(openApiDefinition.getJsonPointer().getJsonPointer()));
    }

    private void initializeJsonDocuments() throws IOException {
        for (SourceDefinition openApiSource : result.getSrc().values()) {
            JsonNode baseDocument = documents.computeIfAbsent(openApiSource.getFile(), f -> {
                try {
                    return getJsonNodeFromFile(f);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            /*
            For each $ref that is found in the JsonNodes, openapi4j also expects a abs$ref, which is the absolute filepath+jsonpointer to the target.
             */
            addAbsoluteRefs(baseDocument, openApiSource.getFile().getAbsoluteFile());
        }
    }

    protected JsonNode getJsonNodeFromFile(File file) throws IOException {
        ObjectMapper mapper;
        if (result.getSrc().get(file.getAbsolutePath()).isYaml()) {
            mapper = new ObjectMapper(new YAMLFactory());
        } else {
            mapper = new ObjectMapper();
        }
        return mapper.readTree(file);
    }
}
