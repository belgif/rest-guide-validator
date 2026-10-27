package io.github.belgif.rest.guide.validator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.resource.MapResourceLoader;
import com.networknt.schema.resource.SchemaLoader;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.parser.SourceDefinition;
import io.github.belgif.rest.guide.validator.core.util.SchemaValidator;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.model.OAIContext;
import org.openapi4j.core.model.reference.OpenApi4jTestContext;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ValidatorMigrationTestSupport {
    static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private ValidatorMigrationTestSupport() {
    }

    static Parser.ParserResult parseResource(Class<?> testClass, String relative) {
        File file = new File(testClass.getResource(relative).getFile());
        Parser.ParserResult result = new Parser(file).parse(new ViolationReport());
        assertNotNull(result, relative);
        return result;
    }

    static Map<String, String> resources(Parser.ParserResult result) throws IOException {
        Map<String, String> documents = new HashMap<>();
        for (SourceDefinition source : result.getSrc().values()) {
            ObjectMapper mapper = source.isYaml() ? YAML : JSON;
            JsonNode document = mapper.readTree(source.getFile());
            documents.put(source.getFile().toURI().toString(), JSON.writeValueAsString(document));
        }
        return documents;
    }

    static String location(OpenApiDefinition<?> definition) {
        return definition.getOpenApiFile().getAbsoluteFile().toURI().toString();
    }

    static Observed validateNetworknt(JsonNode schemaNode, JsonNode instance, String location,
                                      Map<String, String> documents) {
        try {
            SchemaRegistry registry = SchemaRegistry.withDialect(Dialects.getOpenApi30(), builder -> {
                builder.schemaRegistryConfig(com.networknt.schema.SchemaRegistryConfig.builder().build());
                builder.schemaLoader(new SchemaLoader(new MapResourceLoader(documents)));
            });
            Schema schema = documents.isEmpty()
                    ? registry.getSchema(SchemaLocation.of(location), schemaNode)
                    : registry.getSchema(SchemaLocation.of(location));
            List<Error> errors = schema.validate(instance);
            return new Observed(errors.isEmpty() ? State.VALID : State.INVALID,
                    errors.stream().map(error -> error.getKeyword() + "@" + error.getInstanceLocation()).toList(),
                    errors.stream().map(Error::getMessage).toList().toString());
        } catch (RuntimeException e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static Observed validateOpenApi4j(JsonNode schemaNode, JsonNode instance, JsonNode document,
                                      String documentLocation) {
        try {
            URL base = new URL(documentLocation);
            return validateOpenApi4j(schemaNode, instance, OpenApi4jTestContext.create(base, document));
        } catch (RuntimeException e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static Observed validateOpenApi4j(JsonNode schemaNode, JsonNode instance, OAIContext context) {
        try {
            ValidationData<Void> data = new ValidationData<>();
            new org.openapi4j.schema.validator.v3.SchemaValidator(
                    new ValidationContext<OAI3>(context), null, schemaNode).validate(instance, data);
            return new Observed(data.isValid() ? State.VALID : State.INVALID, List.of(),
                    data.results().items().stream().map(item -> item.message()).toList().toString());
        } catch (RuntimeException e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static Observed validateCustomizedOpenApi4j(JsonNode document, JsonNode instance) throws IOException {
        Path file = Files.createTempFile("validator-migration-", ".yaml");
        try {
            Files.writeString(file, JSON.writeValueAsString(document));
            Parser.ParserResult result = new Parser(file.toFile()).parse(new ViolationReport());
            if (result == null) {
                return new Observed(State.EXECUTION_FAILURE, List.of(), "OpenAPI document parsing failed");
            }
            SchemaDefinition schema = result.getSchemas().stream()
                    .filter(candidate -> "Target".equals(candidate.getIdentifier()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Target schema was not parsed"));
            ValidationData<Void> data = SchemaValidator.validate(instance, schema);
            return new Observed(data.isValid() ? State.VALID : State.INVALID, List.of(),
                    data.results().items().stream().map(item -> item.message()).toList().toString());
        } catch (ResolutionException | RuntimeException e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(),
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    static Observed observed(boolean valid) {
        return new Observed(valid ? State.VALID : State.INVALID, List.of(), "");
    }

    enum State {
        VALID, INVALID, EXECUTION_FAILURE
    }

    record Observed(State state, List<String> keywordsAndLocations, String message) {
        String csv() {
            return state.name();
        }
    }
}
