package io.github.belgif.rest.guide.validator.core;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.belgif.rest.guide.validator.core.model.ExampleDefinition;
import io.github.belgif.rest.guide.validator.core.model.MediaTypeDefinition;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.ParameterDefinition;
import io.github.belgif.rest.guide.validator.core.model.ResponseHeaderDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.model.helper.BelgifOAI3Context;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.util.SchemaValidator;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.JSON;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.location;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.observed;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.parseResource;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.resources;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.validateNetworknt;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.validateOpenApi4j;
import static org.junit.jupiter.api.Assertions.*;

import io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.Observed;
import io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.State;

class ParserBackedValidatorMigrationComparisonTest {
    private static final Map<String, List<State>> EXPECTED_ENTRYPOINTS = Map.ofEntries(
            Map.entry("schema-name-discriminator:discriminatorMappingsOnSchemaName.yaml#/components/schemas/FirstEvent", pair(State.VALID, State.VALID)),
            Map.entry("external-default:problem-v1.yaml#/components/schemas/Problem/properties/type", pair(State.VALID, State.VALID)),
            Map.entry("example:circularDependency.yaml#/components/schemas/Logos/example", pair(State.INVALID, State.INVALID)),
            Map.entry("example:refAsPropertyName.yaml#/components/schemas/HealthStatus/example", pair(State.VALID, State.VALID)),
            Map.entry("example:refAsPropertyName.yaml#/paths/health/get/responses/200/content/application/json/examples/response", pair(State.VALID, State.VALID)),
            Map.entry("example:refAsPropertyName.yaml#/paths/health/get/responses/503/content/application/problem+json/examples/response", pair(State.VALID, State.VALID)),
            Map.entry("example:refAsPropertyNameInDiscriminator.yaml#/components/schemas/MyFirstSchema/example", pair(State.VALID, State.VALID)),
            Map.entry("example:uri.yaml#/components/schemas/Uri/properties/href/example", pair(State.INVALID, State.INVALID)),
            Map.entry("example:uriReference.yaml#/components/schemas/UriReference/properties/href/example", pair(State.VALID, State.VALID)),
            Map.entry("example:migrationComparison.yaml#/components/schemas/ProbeString/example", pair(State.VALID, State.VALID)),
            Map.entry("enum:enumCounter.yaml#/components/schemas/SomethingWithEnum/properties/myfirstenum:\"CASH\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:enumCounter.yaml#/components/schemas/SomethingWithEnum/properties/myfirstenum:\"WIRETRANSFER\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:enumCounter.yaml#/components/schemas/SomethingWithEnum/properties/myfirstenum:\"CREDITCARD\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:enumCounter.yaml#/components/schemas/SomethingWithEnum/properties/myfirstenum:\"DEBITCARD\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:enumCounter.yaml#/paths/myFirstPath/get/parameters/0/schema:\"NL\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:enumCounter.yaml#/paths/myFirstPath/get/parameters/0/schema:\"FR\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:enumCounter.yaml#/paths/myFirstPath/get/parameters/0/schema:\"CreditCard\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:migrationComparison.yaml#/components/schemas/ProbeString:\"accepted\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:migrationComparison.yaml#/components/schemas/ProbeString:\"also-accepted\"", pair(State.VALID, State.VALID)),
            Map.entry("enum:migrationComparison.yaml#/components/schemas/EnumComposition/oneOf/0:\"shared\"", pair(State.INVALID, State.INVALID)),
            Map.entry("enum:migrationComparison.yaml#/components/schemas/EnumComposition/oneOf/1:\"shared\"", pair(State.INVALID, State.INVALID)),
            Map.entry("enum:migrationComparison.yaml#/components/schemas/InvalidDefault:1", pair(State.VALID, State.VALID)),
            Map.entry("default:migrationComparison.yaml#/components/schemas/ProbeString", pair(State.VALID, State.VALID)),
            Map.entry("default:migrationComparison.yaml#/components/schemas/InvalidDefault", pair(State.INVALID, State.INVALID)),
            Map.entry("schema-name-oneof:migrationComparison.yaml#/components/schemas/SchemaNameChoice", pair(State.INVALID, State.INVALID)),
            Map.entry("reference-oneof:migrationComparison.yaml#/components/schemas/ReferenceChoice", pair(State.VALID, State.INVALID))
    );

    @Test
    void parserLoadedExamplesEnumsAndDefaultsUseAllThreeExistingEntryPoints() throws Exception {
        Set<String> visited = new HashSet<>();
        for (String path : List.of(
                "../rules/exampleValidatorFiles/circularDependency.yaml",
                "../rules/exampleValidatorFiles/refAsPropertyName.yaml",
                "../rules/exampleValidatorFiles/refAsPropertyNameInDiscriminator.yaml",
                "../rules/exampleValidatorFiles/uri.yaml",
                "../rules/exampleValidatorFiles/uriReference.yaml",
                "../rules/migrationComparison.yaml")) {
            compareParserExamples(parseResource(getClass(), path), visited);
        }

        for (String enumPath : List.of("../rules/lineNumberTests/enumCounter.yaml", "../rules/migrationComparison.yaml")) {
            Parser.ParserResult enums = parseResource(getClass(), enumPath);
            List<SchemaDefinition> enumSchemas = enums.getSchemas().stream()
                    .filter(schema -> schema.getJsonNode().has("enum"))
                    .toList();
            assertFalse(enumSchemas.isEmpty(), enumPath);
            Map<String, String> enumResources = resources(enums);
            for (SchemaDefinition schema : enumSchemas) {
                SchemaDefinition highLevel = schema.getHighLevelSchema();
                String schemaLocation = location(highLevel) + "#" + highLevel.getJsonPointer();
                Set<Map.Entry<String, String>> oldViolations = SchemaValidator.getEnumViolations(schema);
                for (JsonNode value : schema.getJsonNode().get("enum")) {
                    Observed newer = validateNetworknt(highLevel.getJsonNode(), value, schemaLocation, enumResources);
                    Observed old = observed(oldViolations.stream()
                            .noneMatch(violation -> violation.getKey().equals(value.toString())));
                    String id = "enum:" + schema.getFullyQualifiedPointer() + ":" + value;
                    assertEntry(id, old, newer, visited);
                }
            }
        }

        Parser.ParserResult defaults = parseResource(getClass(), "../rules/migrationComparison.yaml");
        List<SchemaDefinition> defaultSchemas = defaults.getSchemas().stream()
                .filter(schema -> schema.getJsonNode().has("default"))
                .toList();
        assertFalse(defaultSchemas.isEmpty());
        for (SchemaDefinition defaultSchema : defaultSchemas) {
            Observed oldDefault = observed(SchemaValidator.getDefaultValueViolations(defaultSchema).isEmpty());
            Observed newDefault = validateNetworknt(defaultSchema.getJsonNode(), defaultSchema.getJsonNode().get("default"),
                    location(defaultSchema) + "#" + defaultSchema.getJsonPointer(), resources(defaults));
            assertEntry("default:" + defaultSchema.getFullyQualifiedPointer(), oldDefault, newDefault, visited);
            assertNotEquals(State.EXECUTION_FAILURE, newDefault.state(), newDefault.message());
        }

        Map<String, String> documents = resources(defaults);
        JsonNode discriminatorInstance = JSON.readTree("{\"kind\":\"child\",\"payload\":\"ok\"}");
        for (String[] mapping : List.of(
                new String[]{"SchemaNameChoice", "schema-name-oneof"},
                new String[]{"ReferenceChoice", "reference-oneof"})) {
            SchemaDefinition schema = defaults.getSchemas().stream()
                    .filter(candidate -> mapping[0].equals(candidate.getIdentifier()))
                    .findFirst().orElseThrow();
            Observed old = validateOpenApi4j(schema.getJsonNode(), discriminatorInstance, new BelgifOAI3Context(schema));
            Observed newer = validateNetworknt(schema.getJsonNode(), discriminatorInstance,
                    location(schema) + "#" + schema.getJsonPointer(), documents);
            String id = mapping[1] + ":" + schema.getFullyQualifiedPointer();
            assertEntry(id, old, newer, visited);
            assertNotEquals(State.EXECUTION_FAILURE, old.state(), old.message());
            assertNotEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
        }

        Parser.ParserResult schemaNameFixture = parseResource(getClass(), "../rules/discriminatorMappingsOnSchemaName.yaml");
        SchemaDefinition firstEvent = schemaNameFixture.getSchemas().stream()
                .filter(candidate -> "FirstEvent".equals(candidate.getIdentifier()))
                .findFirst().orElseThrow();
        JsonNode firstEventInstance = JSON.readTree("{\"type\":\"first\",\"data\":{}}");
        Observed oldFirstEvent = validateOpenApi4j(firstEvent.getJsonNode(), firstEventInstance,
                new BelgifOAI3Context(firstEvent));
        Observed newFirstEvent = validateNetworknt(firstEvent.getJsonNode(), firstEventInstance,
                location(firstEvent) + "#" + firstEvent.getJsonPointer(), resources(schemaNameFixture));
        String firstEventId = "schema-name-discriminator:" + firstEvent.getFullyQualifiedPointer();
        assertEntry(firstEventId, oldFirstEvent, newFirstEvent, visited);
        assertNotEquals(State.EXECUTION_FAILURE, oldFirstEvent.state(), oldFirstEvent.message());
        assertNotEquals(State.EXECUTION_FAILURE, newFirstEvent.state(), newFirstEvent.message());

        Set<String> expectedCoreEntries = EXPECTED_ENTRYPOINTS.keySet().stream()
                .filter(id -> !id.startsWith("external-default:"))
                .collect(Collectors.toSet());
        assertEquals(expectedCoreEntries, visited, "Every expected parser entry point must be visited");
    }

    @Test
    void parserLoadedExternalReferenceDefaultsResolveOrFailExplicitly() throws IOException {
        Set<String> visited = new HashSet<>();
        Parser.ParserResult result = parseResource(getClass(), "../rules/referencedFiles/openapi.yaml");
        Map<String, String> resources = resources(result);
        List<SchemaDefinition> defaults = result.getSchemas().stream()
                .filter(schema -> schema.getJsonNode().has("default"))
                .toList();
        assertFalse(defaults.isEmpty());
        for (SchemaDefinition schema : defaults) {
            Observed old = observed(SchemaValidator.getDefaultValueViolations(schema).isEmpty());
            Observed newer = validateNetworknt(schema.getJsonNode(), schema.getJsonNode().get("default"),
                    location(schema) + "#" + schema.getJsonPointer(), resources);
            assertEntry("external-default:" + schema.getFullyQualifiedPointer(), old, newer, visited);
            assertNotEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
        }
        assertEquals(Set.of("external-default:problem-v1.yaml#/components/schemas/Problem/properties/type"),
                visited, "Every expected external-default entry point must be visited");
    }

    private static void compareParserExamples(Parser.ParserResult result, Set<String> visited) throws IOException {
        assertFalse(result.getExamples().isEmpty(), result.getOpenApiFile().getName());
        Map<String, String> documents = resources(result);
        for (ExampleDefinition original : result.getExamples()) {
            ExampleDefinition resolved = (ExampleDefinition) result.resolve(original.getModel());
            OpenApiDefinition<?> parent = resolved.getParent();
            org.eclipse.microprofile.openapi.models.media.Schema schemaModel;
            if (parent instanceof SchemaDefinition schemaDefinition) {
                schemaModel = schemaDefinition.getModel();
            } else if (parent instanceof MediaTypeDefinition mediaTypeDefinition) {
                schemaModel = mediaTypeDefinition.getModel().getSchema();
            } else if (parent instanceof ParameterDefinition parameterDefinition) {
                schemaModel = parameterDefinition.getModel().getSchema();
            } else if (parent instanceof ResponseHeaderDefinition responseHeaderDefinition) {
                schemaModel = responseHeaderDefinition.getModel().getSchema();
            } else {
                throw new AssertionError("Unexpected example parent: " + parent.getClass().getName());
            }
            assertNotNull(schemaModel, resolved.getFullyQualifiedPointer());
            SchemaDefinition schema = (SchemaDefinition) result.resolve(schemaModel);
            JsonNode instance = resolved.getJsonNode();
            if (resolved.isOasExampleObject() && instance.has("value")) {
                instance = instance.get("value");
            }
            String schemaLocation = location(schema) + "#" + schema.getJsonPointer();
            Observed old = observed(SchemaValidator.getExampleViolations(original).isEmpty());
            Observed newer = validateNetworknt(schema.getJsonNode(), instance, schemaLocation, documents);
            assertEntry("example:" + resolved.getFullyQualifiedPointer(), old, newer, visited);
            assertNotEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
        }
    }

    private static void assertEntry(String id, Observed old, Observed newer, Set<String> visited) {
        assertTrue(visited.add(id), "Duplicate parser entry point: " + id);
        List<State> expected = EXPECTED_ENTRYPOINTS.get(id);
        assertNotNull(expected, "No observed outcome baseline recorded for " + id);
        System.out.printf("ENTRY|%s|openapi4j=%s|networknt=%s%n", id, old.csv(), newer.csv());
        assertEquals(expected.get(0), old.state(), "openapi4j outcome for " + id + ": " + old.message());
        assertEquals(expected.get(1), newer.state(), "networknt outcome for " + id + ": " + newer.message());
    }

    private static List<State> pair(State openapi4j, State networknt) {
        return List.of(openapi4j, networknt);
    }
}
