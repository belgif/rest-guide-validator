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
import io.github.belgif.rest.guide.validator.core.model.ExampleDefinition;
import io.github.belgif.rest.guide.validator.core.model.MediaTypeDefinition;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.ParameterDefinition;
import io.github.belgif.rest.guide.validator.core.model.ResponseHeaderDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.model.helper.BelgifOAI3Context;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.parser.SourceDefinition;
import io.github.belgif.rest.guide.validator.core.util.SchemaValidator;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.openapi4j.core.model.OAIContext;
import org.openapi4j.core.model.reference.OpenApi4jTestContext;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorMigrationComparisonTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String TEST_BASE = "file:///networknt-migration/";

    private static final List<Probe> PROBES = List.of(
            probe("oneof-discriminator-overlapping-branches",
                    "{\"type\":\"object\",\"required\":[\"kind\",\"value\"],\"properties\":{\"kind\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}},\"discriminator\":{\"propertyName\":\"kind\",\"mapping\":{\"a\":\"#/components/schemas/A\"}},\"oneOf\":[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]}",
                    "{\"kind\":\"a\",\"value\":\"x\"}"),
            probe("anyof-discriminator-selected-branch-invalid-alternate-valid",
                    "{\"type\":\"object\",\"required\":[\"kind\",\"value\"],\"properties\":{\"kind\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}},\"discriminator\":{\"propertyName\":\"kind\",\"mapping\":{\"a\":\"#/components/schemas/A\"}},\"anyOf\":[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]}",
                    "{\"kind\":\"a\",\"value\":\"x\"}"),
            probe("discriminator-explicit-reference-mapping",
                    discriminatorSchema("{\"a\":\"#/components/schemas/A\"}", "[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]"),
                    "{\"kind\":\"a\",\"a\":\"ok\"}"),
            probe("discriminator-schema-name-shorthand-confounded-control",
                    discriminatorSchema("{\"a\":\"A\"}", "[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]"),
                    "{\"kind\":\"a\",\"a\":\"ok\"}"),
            probe("discriminator-implicit-schema-name",
                    discriminatorSchema("{}", "[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]"),
                    "{\"kind\":\"A\",\"a\":\"ok\"}"),
            probe("discriminator-missing",
                    discriminatorSchema("{\"a\":\"#/components/schemas/A\"}", "[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]"),
                    "{\"a\":\"ok\"}"),
            probe("discriminator-null",
                    discriminatorSchema("{\"a\":\"#/components/schemas/A\"}", "[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]"),
                    "{\"kind\":null,\"a\":\"ok\"}"),
            probe("discriminator-non-string",
                    discriminatorSchema("{\"a\":\"#/components/schemas/A\"}", "[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]"),
                    "{\"kind\":7,\"a\":\"ok\"}"),
            probe("discriminator-unknown",
                    discriminatorSchema("{\"a\":\"#/components/schemas/A\"}", "[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]"),
                    "{\"kind\":\"missing\",\"a\":\"ok\"}"),
            probe("discriminator-unconstrained-missing",
                    isolatedDiscriminatorSchema(),
                    "{\"a\":\"ok\"}"),
            probe("discriminator-unconstrained-null",
                    isolatedDiscriminatorSchema(),
                    "{\"kind\":null,\"a\":\"ok\"}"),
            probe("discriminator-unconstrained-non-string",
                    isolatedDiscriminatorSchema(),
                    "{\"kind\":7,\"a\":\"ok\"}"),
            probe("discriminator-unconstrained-unknown",
                    isolatedDiscriminatorSchema(),
                    "{\"kind\":\"unknown\",\"a\":\"ok\"}"),
            probe("discriminator-child-allof-inherited",
                    "{\"$ref\":\"#/components/schemas/Child\"}",
                    "{\"kind\":\"child\",\"payload\":\"ok\"}"),
            probe("discriminator-child-allof-inherited-unknown",
                    "{\"$ref\":\"#/components/schemas/Child\"}",
                    "{\"kind\":\"unknown\",\"payload\":\"ok\"}"),
            probe("discriminator-child-allof-inherited-missing",
                    "{\"$ref\":\"#/components/schemas/Child\"}",
                    "{\"payload\":\"ok\"}"),
            probe("nested-unrelated-discriminator",
                    "{\"type\":\"object\",\"allOf\":[{\"type\":\"object\",\"properties\":{\"nested\":{\"type\":\"object\",\"properties\":{\"kind\":{\"type\":\"string\"}},\"discriminator\":{\"propertyName\":\"kind\"}}}}]}",
                    "{\"nested\":{\"kind\":\"x\"}}"),
            probe("nullable-string-null",
                    "{\"type\":\"string\",\"nullable\":true}",
                    "null"),
            probe("nullable-enum-excludes-null",
                    "{\"type\":\"string\",\"nullable\":true,\"enum\":[\"x\"]}",
                    "null"),
            probe("nullable-oneof-type-less-composition",
                    "{\"nullable\":true,\"oneOf\":[{\"type\":\"string\"},{\"type\":\"integer\"}]}",
                    "null"),
            probe("nullable-allof-composition",
                    "{\"nullable\":true,\"allOf\":[{\"type\":\"string\"}]}",
                    "null"),
            probe("type-less-schema-null",
                    "{\"description\":\"no type constraint\"}",
                    "null"),
            probe("required-property-present-null",
                    "{\"type\":\"object\",\"required\":[\"value\"]}",
                    "{\"value\":null}"),
            probe("required-property-absent",
                    "{\"type\":\"object\",\"required\":[\"value\"]}",
                    "{}"),
            probe("int32-upper-bound",
                    "{\"type\":\"integer\",\"format\":\"int32\"}",
                    "2147483648"),
            probe("int32-lower-bound",
                    "{\"type\":\"integer\",\"format\":\"int32\"}",
                    "-2147483649"),
            probe("int64-upper-bound",
                    "{\"type\":\"integer\",\"format\":\"int64\"}",
                    "9223372036854775808"),
            probe("byte-valid-base64",
                    "{\"type\":\"string\",\"format\":\"byte\"}",
                    "\"SGVsbG8=\""),
            probe("byte-invalid-base64",
                    "{\"type\":\"string\",\"format\":\"byte\"}",
                    "\"not base64!\""),
            probe("date-valid",
                    "{\"type\":\"string\",\"format\":\"date\"}",
                    "\"2024-02-29\""),
            probe("date-invalid",
                    "{\"type\":\"string\",\"format\":\"date\"}",
                    "\"2023-02-29\""),
            probe("date-time-invalid",
                    "{\"type\":\"string\",\"format\":\"date-time\"}",
                    "\"2024-01-01 12:00:00\""),
            probe("uri-relative-value",
                    "{\"type\":\"string\",\"format\":\"uri\"}",
                    "\"./documents/123\""),
            probe("uri-reference-relative-value",
                    "{\"type\":\"string\",\"format\":\"uri-reference\"}",
                    "\"./documents/123\""),
            probe("exclusive-minimum-equality",
                    "{\"type\":\"number\",\"minimum\":1,\"exclusiveMinimum\":true}",
                    "1"),
            probe("exclusive-maximum-equality",
                    "{\"type\":\"number\",\"maximum\":1,\"exclusiveMaximum\":true}",
                    "1"),
            probe("additional-properties-with-allof",
                    "{\"type\":\"object\",\"allOf\":[{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}},{\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"}}}],\"additionalProperties\":false}",
                    "{\"a\":\"x\",\"b\":\"y\"}"),
            probe("read-only-context-neutral",
                    "{\"type\":\"object\",\"required\":[\"value\"],\"properties\":{\"value\":{\"type\":\"string\",\"readOnly\":true}}}",
                    "{\"value\":\"present\"}"),
            probe("write-only-context-neutral",
                    "{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"string\",\"writeOnly\":true}}}",
                    "{\"value\":\"secret\"}"),
            probe("ref-property-name-literal-dollar-ref",
                    "{\"type\":\"object\",\"properties\":{\"$ref\":{\"type\":\"string\"}},\"required\":[\"$ref\"]}",
                    "{\"$ref\":\"literal\"}"),
            probe("json-pointer-escaped-component",
                    "{\"$ref\":\"#/components/schemas/A~1B~0C\"}",
                    "\"resolved\"")
    );
    private static final Map<String, List<State>> EXPECTED_MATRIX = Map.ofEntries(
            Map.entry("oneof-discriminator-overlapping-branches", pair(State.VALID, State.INVALID)),
            Map.entry("anyof-discriminator-selected-branch-invalid-alternate-valid", pair(State.INVALID, State.VALID)),
            Map.entry("discriminator-explicit-reference-mapping", pair(State.VALID, State.VALID)),
            Map.entry("discriminator-schema-name-shorthand-confounded-control", pair(State.INVALID, State.INVALID)),
            Map.entry("discriminator-implicit-schema-name", pair(State.VALID, State.VALID)),
            Map.entry("discriminator-missing", pair(State.INVALID, State.INVALID)),
            Map.entry("discriminator-null", pair(State.INVALID, State.INVALID)),
            Map.entry("discriminator-non-string", pair(State.INVALID, State.INVALID)),
            Map.entry("discriminator-unknown", pair(State.INVALID, State.INVALID)),
            Map.entry("discriminator-unconstrained-missing", pair(State.INVALID, State.VALID)),
            Map.entry("discriminator-unconstrained-null", pair(State.INVALID, State.VALID)),
            Map.entry("discriminator-unconstrained-non-string", pair(State.VALID, State.VALID)),
            Map.entry("discriminator-unconstrained-unknown", pair(State.INVALID, State.INVALID)),
            Map.entry("discriminator-child-allof-inherited", pair(State.VALID, State.VALID)),
            Map.entry("discriminator-child-allof-inherited-unknown", pair(State.INVALID, State.VALID)),
            Map.entry("discriminator-child-allof-inherited-missing", pair(State.INVALID, State.VALID)),
            Map.entry("nested-unrelated-discriminator", pair(State.VALID, State.VALID)),
            Map.entry("nullable-string-null", pair(State.VALID, State.VALID)),
            Map.entry("nullable-enum-excludes-null", pair(State.INVALID, State.VALID)),
            Map.entry("nullable-oneof-type-less-composition", pair(State.INVALID, State.VALID)),
            Map.entry("nullable-allof-composition", pair(State.INVALID, State.VALID)),
            Map.entry("type-less-schema-null", pair(State.INVALID, State.VALID)),
            Map.entry("required-property-present-null", pair(State.VALID, State.VALID)),
            Map.entry("required-property-absent", pair(State.INVALID, State.INVALID)),
            Map.entry("int32-upper-bound", pair(State.INVALID, State.VALID)),
            Map.entry("int32-lower-bound", pair(State.INVALID, State.VALID)),
            Map.entry("int64-upper-bound", pair(State.INVALID, State.VALID)),
            Map.entry("byte-valid-base64", pair(State.VALID, State.VALID)),
            Map.entry("byte-invalid-base64", pair(State.INVALID, State.VALID)),
            Map.entry("date-valid", pair(State.VALID, State.VALID)),
            Map.entry("date-invalid", pair(State.VALID, State.INVALID)),
            Map.entry("date-time-invalid", pair(State.INVALID, State.INVALID)),
            Map.entry("uri-relative-value", pair(State.INVALID, State.INVALID)),
            Map.entry("uri-reference-relative-value", pair(State.VALID, State.VALID)),
            Map.entry("exclusive-minimum-equality", pair(State.INVALID, State.INVALID)),
            Map.entry("exclusive-maximum-equality", pair(State.INVALID, State.INVALID)),
            Map.entry("additional-properties-with-allof", pair(State.INVALID, State.INVALID)),
            Map.entry("read-only-context-neutral", pair(State.VALID, State.VALID)),
            Map.entry("write-only-context-neutral", pair(State.VALID, State.VALID)),
            Map.entry("ref-property-name-literal-dollar-ref", pair(State.VALID, State.VALID)),
            Map.entry("json-pointer-escaped-component", pair(State.VALID, State.VALID))
    );
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

    private static Probe probe(String id, String schema, String instance) {
        try {
            return new Probe(id, JSON.readTree(schema), JSON.readTree(instance));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String discriminatorSchema(String mapping, String alternatives) {
        return "{\"type\":\"object\",\"required\":[\"kind\"],\"properties\":{\"kind\":{\"type\":\"string\"},\"a\":{\"type\":\"string\"},\"b\":{\"type\":\"integer\"}},\"discriminator\":{\"propertyName\":\"kind\",\"mapping\":" + mapping + "},\"oneOf\":" + alternatives + "}";
    }

    private static String isolatedDiscriminatorSchema() {
        return "{\"type\":\"object\",\"properties\":{\"kind\":{},\"a\":{\"type\":\"string\"}},\"discriminator\":{\"propertyName\":\"kind\",\"mapping\":{\"a\":\"#/components/schemas/A\"}},\"oneOf\":[{\"$ref\":\"#/components/schemas/A\"},{\"$ref\":\"#/components/schemas/B\"}]}";
    }

    @TestFactory
    List<DynamicTest> targetedOpenApi30Matrix() {
        assertEquals(PROBES.size(), EXPECTED_MATRIX.size());
        long oldRejectsNewAccepts = EXPECTED_MATRIX.values().stream()
                .filter(outcomes -> outcomes.get(0) == State.INVALID && outcomes.get(1) == State.VALID).count();
        long oldAcceptsNewRejects = EXPECTED_MATRIX.values().stream()
                .filter(outcomes -> outcomes.get(0) == State.VALID && outcomes.get(1) == State.INVALID).count();
        assertEquals(13, oldRejectsNewAccepts);
        assertEquals(2, oldAcceptsNewRejects);
        System.out.printf("SUMMARY|matrix=%d|same=%d|openapi4j-invalid-networknt-valid=%d|openapi4j-valid-networknt-invalid=%d%n",
                PROBES.size(), PROBES.size() - oldRejectsNewAccepts - oldAcceptsNewRejects,
                oldRejectsNewAccepts, oldAcceptsNewRejects);
        return PROBES.stream()
                .map(probe -> DynamicTest.dynamicTest(probe.id(), () -> {
                    JsonNode document = probeDocument(probe);
                    String documentLocation = TEST_BASE + probe.id() + ".json";
                    Map<String, String> probeResources = Map.of(documentLocation, JSON.writeValueAsString(document));
                    String schemaLocation = documentLocation + "#/components/schemas/Target";
                    Observed old = validateOpenApi4j(probe.schema(), probe.instance(), document, documentLocation);
                    Observed newer = validateNetworknt(probe.schema(), probe.instance(), schemaLocation, probeResources);
                    System.out.printf("MATRIX|%s|openapi4j=%s|networknt=%s%n", probe.id(), old.csv(), newer.csv());
                    if (Set.of("anyof-discriminator-selected-branch-invalid-alternate-valid",
                            "discriminator-schema-name-shorthand-confounded-control", "discriminator-implicit-schema-name",
                            "required-property-absent", "exclusive-minimum-equality", "uri-relative-value",
                            "additional-properties-with-allof", "nullable-enum-excludes-null").contains(probe.id())) {
                        System.out.printf("DETAIL|%s|openapi4j=%s|networknt=%s|networknt-keywords-locations=%s%n",
                                probe.id(), old.message(), newer.message(), newer.keywordsAndLocations());
                    }
                    assertNotEquals(State.UNCLASSIFIED, old.state());
                    assertNotEquals(State.UNCLASSIFIED, newer.state());
                    assertNotEquals(State.EXECUTION_FAILURE, old.state(), old.message());
                    assertNotEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
                    assertPair(EXPECTED_MATRIX.get(probe.id()), old, newer, probe.id());
                }))
                .toList();
    }

    @Test
    void parserLoadedExamplesEnumsAndDefaultsUseAllThreeExistingEntryPoints() throws Exception {
        Set<String> visited = new java.util.HashSet<>();
        for (String path : List.of(
                "../rules/exampleValidatorFiles/circularDependency.yaml",
                "../rules/exampleValidatorFiles/refAsPropertyName.yaml",
                "../rules/exampleValidatorFiles/refAsPropertyNameInDiscriminator.yaml",
                "../rules/exampleValidatorFiles/uri.yaml",
                "../rules/exampleValidatorFiles/uriReference.yaml",
                "../rules/migrationComparison.yaml")) {
            compareParserExamples(parseResource(path), visited);
        }

        for (String enumPath : List.of("../rules/lineNumberTests/enumCounter.yaml", "../rules/migrationComparison.yaml")) {
            Parser.ParserResult enums = parseResource(enumPath);
            List<SchemaDefinition> enumSchemas = enums.getSchemas().stream()
                    .filter(s -> s.getJsonNode().has("enum"))
                    .toList();
            assertFalse(enumSchemas.isEmpty(), enumPath);
            Map<String, String> enumResources = resources(enums);
            for (SchemaDefinition schema : enumSchemas) {
                SchemaDefinition highLevel = schema.getHighLevelSchema();
                String schemaLocation = location(highLevel) + "#" + highLevel.getJsonPointer();
                Set<Map.Entry<String, String>> oldViolations = SchemaValidator.getEnumViolations(schema);
                for (JsonNode value : schema.getJsonNode().get("enum")) {
                    Observed newer = validateNetworknt(highLevel.getJsonNode(), value, schemaLocation, enumResources);
                    Observed old = observed(oldViolations.stream().noneMatch(violation -> violation.getKey().equals(value.toString())));
                    String id = "enum:" + schema.getFullyQualifiedPointer() + ":" + value;
                    assertEntry(id, old, newer, visited);
                }
            }
        }

        Parser.ParserResult defaults = parseResource("../rules/migrationComparison.yaml");
        List<SchemaDefinition> defaultSchemas = defaults.getSchemas().stream()
                .filter(s -> s.getJsonNode().has("default"))
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

        Parser.ParserResult schemaNameFixture = parseResource("../rules/discriminatorMappingsOnSchemaName.yaml");
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
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(expectedCoreEntries, visited, "Every expected parser entry point must be visited");
    }

    @Test
    void parserLoadedExternalReferenceDefaultsResolveOrFailExplicitly() throws IOException {
        Set<String> visited = new java.util.HashSet<>();
        for (String relative : List.of("../rules/referencedFiles/openapi.yaml")) {
            Parser.ParserResult result = parseResource(relative);
            Map<String, String> resources = resources(result);
            List<SchemaDefinition> defaults = result.getSchemas().stream()
                    .filter(schema -> schema.getJsonNode().has("default"))
                    .toList();
            assertFalse(defaults.isEmpty(), relative);
            for (SchemaDefinition schema : defaults) {
                Observed old = observed(SchemaValidator.getDefaultValueViolations(schema).isEmpty());
                Observed newer = validateNetworknt(schema.getJsonNode(), schema.getJsonNode().get("default"),
                        location(schema) + "#" + schema.getJsonPointer(), resources);
                assertEntry("external-default:" + schema.getFullyQualifiedPointer(), old, newer, visited);
                assertNotEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
            }
        }
        assertEquals(Set.of("external-default:problem-v1.yaml#/components/schemas/Problem/properties/type"),
                visited, "Every expected external-default entry point must be visited");
    }

    @Test
    void unresolvedReferencesAreExecutionFailuresNotInvalidInstances() throws IOException {
        JsonNode schema = JSON.readTree("{\"$ref\":\"#/components/schemas/Missing\"}");
        JsonNode instance = JSON.readTree("\"anything\"");
        String documentLocation = TEST_BASE + "unresolved.json";
        JsonNode document = probeDocument(probe("unresolved", "{\"$ref\":\"#/components/schemas/Missing\"}", "\"anything\""));
        Map<String, String> resources = Map.of(documentLocation, JSON.writeValueAsString(document));
        Observed old = validateOpenApi4j(schema, instance, document, documentLocation);
        Observed newer = validateNetworknt(schema, instance, documentLocation + "#/components/schemas/Target", resources);
        assertEquals(State.EXECUTION_FAILURE, old.state(), old.message());
        assertEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
    }

    private static void compareParserExamples(Parser.ParserResult result, Set<String> visited) throws IOException {
        assertFalse(result.getExamples().isEmpty(), result.getOpenApiFile().getName());
        Map<String, String> resources = resources(result);
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
            Observed newer = validateNetworknt(schema.getJsonNode(), instance, schemaLocation, resources);
            assertEntry("example:" + resolved.getFullyQualifiedPointer(), old, newer, visited);
            assertNotEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
        }
    }

    private static void logEntryPoint(String id, Observed old, Observed newer) {
        System.out.printf("ENTRY|%s|openapi4j=%s|networknt=%s%n", id, old.csv(), newer.csv());
    }

    private static List<State> pair(State old, State newer) {
        return List.of(old, newer);
    }

    private static void assertEntry(String id, Observed old, Observed newer) {
        logEntryPoint(id, old, newer);
        assertPair(EXPECTED_ENTRYPOINTS.get(id), old, newer, id);
    }

    private static void assertEntry(String id, Observed old, Observed newer, Set<String> visited) {
        assertTrue(visited.add(id), "Duplicate parser entry point: " + id);
        assertEntry(id, old, newer);
    }

    private static void assertPair(List<State> expected, Observed old, Observed newer, String id) {
        assertNotNull(expected, "No observed outcome baseline recorded for " + id);
        assertEquals(expected.get(0), old.state(), "openapi4j outcome for " + id + ": " + old.message());
        assertEquals(expected.get(1), newer.state(), "networknt outcome for " + id + ": " + newer.message());
    }

    private static Parser.ParserResult parseResource(String relative) {
        File file = new File(ValidatorMigrationComparisonTest.class.getResource(relative).getFile());
        Parser.ParserResult result = new Parser(file).parse(new ViolationReport());
        assertNotNull(result, relative);
        return result;
    }

    private static Map<String, String> resources(Parser.ParserResult result) throws IOException {
        Map<String, String> documents = new HashMap<>();
        for (SourceDefinition source : result.getSrc().values()) {
            ObjectMapper mapper = source.isYaml() ? YAML : JSON;
            JsonNode document = mapper.readTree(source.getFile());
            documents.put(source.getFile().toURI().toString(), JSON.writeValueAsString(document));
        }
        return documents;
    }

    private static String location(OpenApiDefinition<?> definition) {
        return definition.getOpenApiFile().getAbsoluteFile().toURI().toString();
    }

    private static Observed validateNetworknt(JsonNode schemaNode, JsonNode instance, String location, Map<String, String> documents) {
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
                    errors.stream().map(error -> error.getMessage()).toList().toString());
        } catch (RuntimeException e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(), e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static Observed validateOpenApi4j(JsonNode schemaNode, JsonNode instance, JsonNode document, String documentLocation) {
        try {
            URL base = new URL(documentLocation);
            return validateOpenApi4j(schemaNode, instance, OpenApi4jTestContext.create(base, document));
        } catch (RuntimeException e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(), e.getClass().getSimpleName() + ": " + e.getMessage());
        } catch (Exception e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(), e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static Observed validateOpenApi4j(JsonNode schemaNode, JsonNode instance, OAIContext context) {
        try {
            ValidationData<Void> data = new ValidationData<>();
            new org.openapi4j.schema.validator.v3.SchemaValidator(new ValidationContext<OAI3>(context), null, schemaNode)
                    .validate(instance, data);
            return new Observed(data.isValid() ? State.VALID : State.INVALID, List.of(),
                    data.results().items().stream().map(item -> item.message()).toList().toString());
        } catch (RuntimeException e) {
            return new Observed(State.EXECUTION_FAILURE, List.of(), e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private static JsonNode probeDocument(Probe probe) {
        JsonNode document = JSON.createObjectNode();
        var components = (com.fasterxml.jackson.databind.node.ObjectNode) document;
        var schemas = components.putObject("components").putObject("schemas");
        schemas.set("Target", probe.schema());
        if (probe.id().startsWith("discriminator-unconstrained-")) {
            schemas.set("A", requiredPropertySchema("a", "string"));
            schemas.set("B", requiredPropertySchema("a", "integer"));
        } else if (probe.id().startsWith("discriminator-child-allof-inherited")) {
            var base = JSON.createObjectNode();
            base.put("type", "object");
            base.putObject("properties").putObject("kind");
            base.putObject("discriminator").put("propertyName", "kind")
                    .putObject("mapping").put("child", "#/components/schemas/Child");
            schemas.set("Base", base);
            var child = JSON.createObjectNode();
            child.putArray("allOf").addObject().put("$ref", "#/components/schemas/Base");
            child.withArray("allOf").addObject().put("type", "object")
                    .putObject("properties").putObject("payload").put("type", "string");
            schemas.set("Child", child);
        } else if (probe.id().equals("oneof-discriminator-overlapping-branches")) {
            schemas.set("A", objectSchema("a", "string"));
            schemas.set("B", objectSchema("a", "string"));
        } else if (probe.id().equals("anyof-discriminator-selected-branch-invalid-alternate-valid")) {
            schemas.set("A", objectSchema("a", "integer"));
            schemas.set("B", objectSchema("a", "string"));
        } else if (probe.id().equals("discriminator-implicit-schema-name")) {
            schemas.set("A", objectSchema("A", "string"));
            schemas.set("B", objectSchema("b", "string"));
        } else {
            schemas.set("A", objectSchema("a", "string"));
            schemas.set("B", objectSchema("b", "string"));
        }
        schemas.set("A/B~C", JSON.createObjectNode().put("type", "string"));
        return document;
    }

    private static JsonNode objectSchema(String kind, String valueType) {
        var schema = JSON.createObjectNode();
        schema.put("type", "object");
        schema.putObject("properties").putObject("kind").putArray("enum").add(kind);
        schema.with("properties").putObject("value").put("type", valueType);
        schema.putArray("required").add("kind");
        return schema;
    }

    private static JsonNode requiredPropertySchema(String property, String type) {
        var schema = JSON.createObjectNode();
        schema.put("type", "object");
        schema.putArray("required").add(property);
        schema.putObject("properties").putObject(property).put("type", type);
        return schema;
    }

    private static Observed observed(boolean valid) {
        return new Observed(valid ? State.VALID : State.INVALID, List.of(), "");
    }

    private record Probe(String id, JsonNode schema, JsonNode instance) {
    }

    private enum State {
        VALID, INVALID, EXECUTION_FAILURE, UNCLASSIFIED
    }

    private record Observed(State state, List<String> keywordsAndLocations, String message) {
        String csv() {
            return state.name();
        }
    }
}
