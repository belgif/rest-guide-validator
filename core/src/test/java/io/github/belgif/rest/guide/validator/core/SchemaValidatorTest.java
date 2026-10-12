package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.ExampleDefinition;
import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import io.github.belgif.rest.guide.validator.core.util.SchemaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SchemaValidatorTest {
    @TempDir
    Path directory;

    @Test
    void testCircularDependency() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/circularDependency.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples();
        var example = examples.stream().findAny();
        assertTrue(example.isPresent());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(example.get()));
    }

    @Test
    void testSameComponentNameInRef() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/sameName.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples().stream().filter(exampleDefinition -> exampleDefinition.getDefinitionType().equals(OpenApiDefinition.DefinitionType.INLINE)).collect(Collectors.toSet());

        assertFalse(examples.isEmpty());
        examples.forEach(example -> assertTrue(SchemaValidator.getExampleViolations(example).isPresent()));
    }

    @Test
    void testValidationOnDeeperSchemas() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/parameter.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var examples = result.getExamples().stream().filter(exampleDefinition -> exampleDefinition.getJsonPointer().getJsonPointer().equals("/paths/~1encryptionKeys~1{id}/parameters/0/example")).collect(Collectors.toList());

        assertEquals(1, examples.size());
        assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertEquals(1, SchemaValidator.getExampleViolations(examples.get(0)).get().lines().count());
    }

    @Test
    void testDiscriminatorMappingOnSchemaName() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/discriminatorMappingsOnSchemaName.yaml").getFile());
        var result = new Parser(file).parse(oas);

        for (SchemaDefinition schemaDefinition : result.getSchemas()) {
            if (schemaDefinition.getModel().getEnumeration() != null) {
                assertTrue(SchemaValidator.getEnumViolations(schemaDefinition).isEmpty());
            }
        }
    }

    @Test
    void testRefAsPropertyName() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/refAsPropertyName.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().filter(def -> def.getPrintableJsonPointer().equals("/components/schemas/HealthStatus/example")).toList();

        assertEquals(1, examples.size());
        assertTrue(SchemaValidator.getExampleViolations(examples.get(0)).isEmpty());
    }

    @Test
    void testRefAsPropertyNameOfDiscriminator() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/refAsPropertyNameInDiscriminator.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        assertTrue(SchemaValidator.getExampleViolations(examples.get(0)).isEmpty());
    }

    @Test
    void testUriReference() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/uriReference.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        Optional<String> uriReferenceViolation = assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertTrue(uriReferenceViolation.isEmpty());
    }

    @Test
    void testUri() {
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/uri.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(1, examples.size());
        Optional<String> uriViolation = assertDoesNotThrow(() -> SchemaValidator.getExampleViolations(examples.get(0)));
        assertTrue(uriViolation.isPresent());
    }

    @Test
    void testUriWithTrailingForwardSlash (){
        var oas = new ViolationReport();
        var file = new File(this.getClass().getResource("../rules/exampleValidatorFiles/uriWithTrailingForwardSlash.yaml").getFile());
        var result = new Parser(file).parse(oas);
        var examples = result.getExamples().stream().toList();

        assertEquals(2, examples.size());

        for (ExampleDefinition example : examples) {
            assertEquals(example.getOpenApiFile().getName().equals("referencedFileWithExample.yaml"),
                    SchemaValidator.getExampleViolations(example).isEmpty());
        }
    }

    @Test
    void testStrictUriAndUriReference() throws IOException {
        for (String format : new String[]{"uri", "uri-reference"}) {
            for (String value : new String[]{"https://example.org/path", "/relative", "http://bad host/",
                    "https://example.org/caf\u00e9", "https://example.org/?q=[value]"}) {
                var schema = subject("type: string\nformat: " + format + "\ndefault: '" + value + "'\n");
                boolean valid = value.equals("https://example.org/path") ||
                        (format.equals("uri-reference") && value.equals("/relative"));
                assertEquals(valid, SchemaValidator.getDefaultValueViolations(schema).isEmpty(), format + ": " + value);
            }
        }
    }

    @Test
    void testNativeNullable() throws IOException {
        assertTrue(SchemaValidator.getDefaultValueViolations(subject("""
                type: string
                nullable: true
                default: null
                """)).isEmpty());
        assertTrue(SchemaValidator.getDefaultValueViolations(subject("""
                type: string
                nullable: false
                default: null
                """)).isPresent());
        assertTrue(SchemaValidator.getDefaultValueViolations(subject("""
                type: string
                nullable: true
                enum: [allowed]
                default: null
                """)).isEmpty());
        for (String keyword : new String[]{"allOf", "oneOf", "anyOf"}) {
            assertTrue(SchemaValidator.getDefaultValueViolations(subject("""
                    nullable: true
                    %s:
                      - type: string
                      - type: integer
                    default: null
                    """.formatted(keyword))).isEmpty(), keyword);
        }
        assertTrue(SchemaValidator.getDefaultValueViolations(subject("""
                type: object
                nullable: true
                properties:
                  name:
                    type: string
                default: {name: null}
                """)).isPresent(), "Nullable must not leak into object properties");
    }

    @Test
    void testNullableReferencesAndRequiredProperties() throws IOException {
        var result = parseSchemas("""
                Subject:
                  type: object
                  properties:
                    nullable:
                      $ref: '#/components/schemas/NullableString'
                    nonNullable:
                      $ref: '#/components/schemas/String'
                      nullable: true
                  default: {nullable: null, nonNullable: null}
                  example: {nullable: null, nonNullable: valid}
                NullableString: {type: string, nullable: true}
                String: {type: string}
                """);
        assertTrue(SchemaValidator.getExampleViolations(result.getExamples().iterator().next()).isEmpty());
        String violation = SchemaValidator.getDefaultValueViolations(schema(result, "Subject")).orElseThrow();
        assertEquals(1, violation.lines().count());
        assertTrue(violation.startsWith("nonNullable: "), violation);
        assertTrue(SchemaValidator.getDefaultValueViolations(subject("""
                type: object
                required: [name]
                properties:
                  name: {type: string, nullable: true}
                default: {}
                """)).isPresent(), "A nullable property is still required");
    }

    @Test
    void testCrossFileReferencesWithIdenticalComponentNames() throws IOException {
        Files.writeString(directory.resolve("other.yaml"), """
                openapi: 3.0.3
                info: {title: Referenced schema, version: '1'}
                paths: {}
                components:
                  schemas:
                    Subject:
                      type: object
                      properties:
                        value: {type: integer}
                """);
        var result = parseSchemas("""
                Subject:
                  type: object
                  properties:
                    value: {type: string}
                    other: {$ref: 'other.yaml/#/components/schemas/Subject'}
                  default: {value: valid, other: {value: wrong}}
                  example: {value: valid, other: {value: 123}}
                """);
        assertTrue(SchemaValidator.getExampleViolations(result.getExamples().iterator().next()).isEmpty());
        var subject = result.getSchemas().stream().filter(def ->
                "Subject".equals(def.getIdentifier()) && def.getOpenApiFile().equals(result.getOpenApiFile()))
                .findFirst().orElseThrow();
        String violation = SchemaValidator.getDefaultValueViolations(subject).orElseThrow();
        assertTrue(violation.startsWith("other.value: "), violation);
        assertTrue(violation.contains(subject.getOpenApiFile().getName() + "#/components/schemas/Subject : "), violation);
    }

    @Test
    void testNullableDiscriminatorCompositions() throws IOException {
        for (String keyword : new String[]{"oneOf", "anyOf"}) {
            var schema = schema(parseSchemas("""
                    Subject:
                      nullable: true
                      %s:
                        - $ref: '#/components/schemas/Cat'
                        - $ref: '#/components/schemas/Dog'
                      discriminator: {propertyName: kind}
                      default: null
                    Cat: {type: object}
                    Dog: {type: object}
                    """.formatted(keyword)), "Subject");
            assertTrue(SchemaValidator.getDefaultValueViolations(schema).isEmpty(), keyword);
        }
        var schema = schema(parseSchemas("""
                Subject:
                  nullable: true
                  allOf:
                    - $ref: '#/components/schemas/Pet'
                  default: null
                Pet:
                  type: object
                  discriminator:
                    propertyName: kind
                    mapping: {subject: Subject}
                """), "Subject");
        assertTrue(SchemaValidator.getDefaultValueViolations(schema).isEmpty(), "allOf");
    }

    @Test
    void testRecursiveReferencesValidateNestedData() throws IOException {
        var result = parseSchemas("""
                Subject:
                  type: object
                  properties:
                    value: {type: integer}
                    next: {$ref: '#/components/schemas/Other'}
                  default: {value: 1, next: {value: 2, next: {value: wrong}}}
                Other:
                  type: object
                  properties:
                    value: {type: integer}
                    next: {$ref: '#/components/schemas/Subject'}
                  example: {value: 1, next: {value: 2}}
                """);
        assertTrue(SchemaValidator.getExampleViolations(result.getExamples().iterator().next()).isEmpty());
        String violation = SchemaValidator.getDefaultValueViolations(schema(result, "Subject")).orElseThrow();
        assertTrue(violation.startsWith("next.next.value: "), violation);
        assertTrue(violation.contains(" In Schema: "), violation);
        assertTrue(violation.contains("#/components/schemas/Subject : "), violation);
        assertTrue(violation.endsWith("<type>"), violation);
    }

    @Test
    void testEnumAndExampleApiRetainContext() throws IOException {
        var result = parseSchemas("""
                Subject:
                  type: integer
                  minimum: 2
                  enum: [1, 2]
                  default: 1
                  example: 1
                """);
        var schema = schema(result, "Subject");
        var enums = SchemaValidator.getEnumViolations(schema);
        assertEquals(1, enums.size());
        assertEquals("1", enums.iterator().next().getKey());
        var example = SchemaValidator.getExampleViolations(result.getExamples().iterator().next()).orElseThrow();
        assertEquals(SchemaValidator.getDefaultValueViolations(schema).orElseThrow(), example);
        assertEquals(enums.iterator().next().getValue(), example);
        assertTrue(example.contains("#/components/schemas/Subject : <minimum>"), example);
    }

    @Test
    void testDiscriminatorSelectsOnlyMappedOneAnyOfBranch() throws IOException {
        for (String keyword : new String[]{"oneOf", "anyOf"}) {
            for (String mapping : new String[]{"Cat", "#/components/schemas/Cat"}) {
                var result = parseSchemas("""
                        Subject:
                          %s:
                            - $ref: '#/components/schemas/Cat'
                            - $ref: '#/components/schemas/Dog'
                          discriminator:
                            propertyName: kind
                            mapping: {cat: '%s'}
                          default: {kind: cat, name: 123}
                          example: {kind: cat, name: valid}
                        Cat:
                          type: object
                          properties:
                            name: {type: string}
                        Dog:
                          type: object
                        """.formatted(keyword, mapping));
                assertTrue(SchemaValidator.getExampleViolations(result.getExamples().iterator().next()).isEmpty(),
                        "Even when both branches match, select Cat: " + keyword);
                String violation = SchemaValidator.getDefaultValueViolations(schema(result, "Subject")).orElseThrow();
                assertTrue(violation.startsWith("name: "), violation);
                assertTrue(violation.contains("<" + keyword + ">"), violation);
                assertTrue(violation.endsWith("<type>"), violation);
                assertEquals(mapping, schema(result, "Subject").getJsonNode()
                        .path("discriminator").path("mapping").path("cat").asText());
            }
            for (String value : new String[]{"{kind: Cat}", "{kind: unknown}", "{}"}) {
                var schema = schema(parseSchemas("""
                        Subject:
                          %s:
                            - $ref: '#/components/schemas/Cat'
                          discriminator: {propertyName: kind}
                          default: %s
                        Cat: {type: object}
                        """.formatted(keyword, value)), "Subject");
                assertEquals(value.equals("{kind: Cat}"),
                        SchemaValidator.getDefaultValueViolations(schema).isEmpty(), keyword + ": " + value);
            }
        }
    }

    @Test
    void testAllOfDiscriminatorFromImmediateParent() throws IOException {
        for (String value : new String[]{"{kind: cat, name: valid}", "{kind: Subject, name: valid}",
                "{kind: cat, name: 123}", "{kind: unknown}", "{}"}) {
            var schema = schema(parseSchemas("""
                    Subject:
                      allOf:
                        - $ref: '#/components/schemas/Pet'
                        - type: object
                          properties:
                            name: {type: string}
                      default: %s
                    Pet:
                      type: object
                      discriminator:
                        propertyName: kind
                        mapping: {cat: Subject}
                    """.formatted(value)), "Subject");
            assertEquals(value.equals("{kind: cat, name: valid}") || value.equals("{kind: Subject, name: valid}"),
                    SchemaValidator.getDefaultValueViolations(schema).isEmpty(), value);
        }
    }

    @Test
    void testAdjacentAllOfDiscriminatorDoesNotSelectAnotherSchema() throws IOException {
        var schema = schema(parseSchemas("""
                Subject:
                  allOf:
                    - type: object
                  discriminator:
                    propertyName: kind
                    mapping: {cat: Cat}
                  default: {kind: unknown}
                Cat: {type: integer}
                """), "Subject");
        assertTrue(SchemaValidator.getDefaultValueViolations(schema).isEmpty());
    }

    private SchemaDefinition subject(String schema) throws IOException {
        return schema(parseSchemas("Subject:\n" + schema.indent(2)), "Subject");
    }

    private Parser.ParserResult parseSchemas(String schemas) throws IOException {
        Path file = Files.createTempFile(directory, "schemas", ".yaml");
        Files.writeString(file, """
                openapi: 3.0.3
                info: {title: Validation regression, version: '1'}
                paths: {}
                components:
                  schemas:
                """ + schemas.indent(4));
        return new Parser(file.toFile()).parse(new ViolationReport());
    }

    private SchemaDefinition schema(Parser.ParserResult result, String name) {
        return result.getSchemas().stream().filter(def -> name.equals(def.getIdentifier())).findFirst().orElseThrow();
    }
}
