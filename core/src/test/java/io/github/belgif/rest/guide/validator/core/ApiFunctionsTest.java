package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.OperationDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.model.helper.MediaType;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ApiFunctionsTest {

    @TempDir
    Path tempDir;

    @Test
    void testUrl() {
        var pattern = "^((https://)|/?)[-a-zA-Z0-9@:%._+~#=]{1,256}[a-zA-Z0-9()]{1,6}/[A-Za-z0-9]*(/[a-z0-9]+([A-Z]?[a-z0-9]+)*)*/v[0-9]+$";
        var url = "https://localhost:8080/REST/v1";
        assertTrue(url.matches(pattern));
        url = "/REST/contactData/v1";
        assertTrue(url.matches(pattern));
    }

    @Test
    void testUrlV3() {
        var pattern = "^((http://localhost/?)|(https://)|/?)[-a-zA-Z0-9@:%._+~#=]{1,256}[a-zA-Z0-9()]{1,6}(/[A-Za-z0-9]*)*(/[a-z0-9]+([A-Z]?[a-z0-9]+)*)*/v[0-9]+$";

        var url = "/api/v3";
        assertTrue(url.matches(pattern));
        url = "myserver/api/v3";
        assertTrue(url.matches(pattern), url);
        url = "/myserver/api/v3";
        assertTrue(url.matches(pattern), url);
        url = "https://myorg/api/v3";
        assertTrue(url.matches(pattern), url);
        url = "http://localhost/api/v3";
        assertTrue(url.matches(pattern), url);
        url = "http://myorg/api/v3";
        assertFalse(url.matches(pattern), url);
        url = "https://myorg/api";
        assertFalse(url.matches(pattern), url);
    }

    @Test
    void isLowerCamelCase() {
        assertTrue(ApiFunctions.isLowerCamelCase("creditCard"));
        assertTrue(ApiFunctions.isLowerCamelCase("creditCard1"));
        assertTrue(ApiFunctions.isLowerCamelCase("creditCardA"));
        assertTrue(ApiFunctions.isLowerCamelCase("cash"));
        assertTrue(ApiFunctions.isLowerCamelCase("cashId"));
        assertFalse(ApiFunctions.isLowerCamelCase("CreditCard"));
        assertFalse(ApiFunctions.isLowerCamelCase("CASH"));
    }

    @Test
    void isUpperKebabCase() {
        assertTrue(ApiFunctions.isUpperKebabCase("This-Is-Test"));
        assertTrue(ApiFunctions.isUpperKebabCase("This-Is-A-Test"));
        assertTrue(ApiFunctions.isUpperKebabCase("X-Is-Test"));
        assertFalse(ApiFunctions.isUpperKebabCase("X-IS-TEST"));
    }

    @Test
    void isNotInSetTest() {
        assertTrue(ApiFunctions.isNotInSet("notInThere", Set.of("ETag", "BelGov-*")));
        assertFalse(ApiFunctions.isNotInSet("In-There", Set.of("In-There", "Something Else", "BelGov-*")));
        assertTrue(ApiFunctions.isNotInSet(null, Set.of("ETag")));
        assertTrue(ApiFunctions.isNotInSet("null", null));
        assertTrue(ApiFunctions.isNotInSet(null, null));
        assertFalse(ApiFunctions.isNotInSet("BelGov-something-something", Set.of("In-There", "Something Else", "BelGov-*")));
    }

    @Test
    void isCompatibleMediaType() {
        List<MediaType> mediaTypes = new ArrayList<>();
        mediaTypes.add(new MediaType("application/json"));
        mediaTypes.add(new MediaType("multipart/*"));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/problem+json", mediaTypes));
        assertFalse(ApiFunctions.isMediaTypeIncluded("application/problem+xml", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/form-data", mediaTypes));
    }

    @Test
    void existsPathWithPathParamAfterTest() {
        var result = parseOpenApi("isCollection.yaml");
        var defs = result.getPathDefinitions();
        var logos = defs.stream().filter(definition -> "/logos".equals(definition.getIdentifier())).findAny();
        assertTrue(logos.isPresent());
        assertTrue(ApiFunctions.existsPathWithPathParamAfter(logos.get().getIdentifier(), result));
    }

    @Test
    void findCallingOperationsTest() {
        var result = parseOpenApi("findCallingOperations.yaml");

        var innerSchema = getSchemaDefinition("MyChildObject", result);
        Set<OperationDefinition> operations = ApiFunctions.findOperationsUsingDefinition(innerSchema, false);
        assertEquals(3, operations.size());
        operations = ApiFunctions.findOperationsUsingDefinition(innerSchema, true);
        assertEquals(1, operations.size());

        var paramSchema = getSchemaDefinition("MyParamObject", result);
        operations = ApiFunctions.findOperationsUsingDefinition(paramSchema, false);
        assertEquals(1, operations.size());
        var operation = operations.iterator().next();
        assertEquals("myPath", operation.getModel().getOperationId());

        var allOfSchema = getSchemaDefinition("FirstImplementation", result);
        operations = ApiFunctions.findOperationsUsingDefinition(allOfSchema, false);
        assertEquals(1, operations.size());
        operation = operations.iterator().next();
        assertEquals("pathWithAllOf", operation.getModel().getOperationId());
    }

    @Test
    void testFindSchemaTypesCombinedAllOfOneOf() {
        var openapi = parseOpenApiFromString("""
                CombinedSchemaAllOfOneOf:
                  type: object
                  allOf:
                    - type: object
                    - properties:
                        a:
                          type: string
                  oneOf:
                    - type: object
                    - type: string
            """
        );
        var schema =  getSchemaDefinition("CombinedSchemaAllOfOneOf", openapi);
        var validationResult = ApiFunctions.findSchemaTypes(schema.getModel(), openapi);
        assertFalse(validationResult.hasConflict());
        assertEquals(1, validationResult.allowedTypes().size());
        assertTrue(validationResult.allowedTypes().contains(Schema.SchemaType.OBJECT));
    }

    @Test
    void testFindSchemaTypesCombinedSchemaAllOfAnyOf() {
        var openapi = parseOpenApiFromString("""
                CombinedSchemaAllOfAnyOf:
                  type: object
                  allOf:
                    - type: object
                  anyOf:
                    - type: object
                    - type: string
            """
        );
        var schema =  getSchemaDefinition("CombinedSchemaAllOfAnyOf", openapi);
        var validationResult = ApiFunctions.findSchemaTypes(schema.getModel(), openapi);
        assertFalse(validationResult.hasConflict());
        assertEquals(1, validationResult.allowedTypes().size());
        assertTrue(validationResult.allowedTypes().contains(Schema.SchemaType.OBJECT));
    }

    @Test
    void testFindSchemaTypesWithRefValid() { //split in test cases, with meaningful name describing the case being tested
        var openapi = parseOpenApiFromString("""
                    SchemaA:
                      allOf:
                        - $ref: "#/components/schemas/StringOrObject"
                        - type: object
                    StringOrObject:
                      oneOf:
                        - type: string
                        - type: object
                """);

        var schema = getSchemaDefinition("SchemaA", openapi);
        var validationResult = ApiFunctions.findSchemaTypes(schema.getModel(), openapi);
        assertFalse(validationResult.hasConflict());
        assertEquals(1, validationResult.allowedTypes().size());
    }

    @Test
    void testFindSchemaTypesWithRefInvalid() { //split in test cases, with meaningful name describing the case being tested
        var openapi = parseOpenApiFromString("""
                    SchemaIntegerAndRef:
                      allOf:
                        - type: integer
                        - $ref: "#/components/schemas/StringOrObject"
                    StringOrObject:
                      oneOf:
                        - type: string
                        - type: object
                """);
        var schema = getSchemaDefinition("SchemaIntegerAndRef", openapi);
        var validationResult = ApiFunctions.findSchemaTypes(schema.getModel(), openapi);
        assertTrue(validationResult.hasConflict());
        assertEquals(0, validationResult.allowedTypes().size());
    }

    @Test
    void testFindSchemaTypesInvalidReference() {
        var openapi = parseOpenApiFromString("""
                    InvalidReferencedSchema:
                        type: object
                        allOf:
                            $ref: "#/components/schemas/SchemaIntegerAndRef"
                    SchemaIntegerAndRef:
                      allOf:
                        - type: integer
                        - $ref: "#/components/schemas/StringOrObject"
                    StringOrObject:
                      oneOf:
                        - type: string
                        - type: object
        """);
        var schema = getSchemaDefinition("InvalidReferencedSchema", openapi);
        var validationResult = ApiFunctions.findSchemaTypes(schema.getModel(), openapi);
        assertFalse(validationResult.hasConflict());
        assertEquals(1, validationResult.allowedTypes().size());
    }


    @Test
    void testFindSchemaTypesInvalid() { //split in test cases, with meaningful name describing the case being testedZ
        var result = parseOpenApiFromString("""
                    RootSchema:
                      allOf:
                        - $ref: "#/components/schemas/SchemaA"
                        - $ref: "#/components/schemas/SchemaB"
                
                    SchemaA:
                      allOf:
                        - $ref: "#/components/schemas/SchemaC"
                        - type: object
                
                    SchemaB:
                      allOf:
                        - type: string
                        - $ref: "#/components/schemas/SchemaC"
                
                    SchemaC:
                      oneOf:
                        - type: string
                        - type: object
                """);

        var rootSchema = getSchemaDefinition("RootSchema", result);
        ApiFunctions.ConflictingSchemaValidation allowedSchemaTypes = assertDoesNotThrow(() -> ApiFunctions.findSchemaTypes(rootSchema.getModel(), result));
        assertTrue(allowedSchemaTypes.hasConflict());
        assertEquals(0, allowedSchemaTypes.allowedTypes().size());
    }

    @Test
    void testFindSchemaTypesWithCircularReference() {
        var result = parseOpenApiFromString("""
                    RootSchema:
                      allOf:
                        - $ref: "#/components/schemas/SchemaA"
                        - $ref: "#/components/schemas/SchemaB"
                
                    SchemaA:
                       type: object
                
                    SchemaB:
                      allOf:
                        - type: string
                        - $ref: "#/components/schemas/RootSchema"
                """);

        var rootSchema = getSchemaDefinition("RootSchema", result);
        assertThrows(IllegalStateException.class, () -> ApiFunctions.findSchemaTypes(rootSchema.getModel(), result));
    }

    @Test
    void testFindSchemaTypesWithComplicatedAllOf() {
        var openapi = parseOpenApiFromString("""
                    AllOfComplicated: # this is valid
                       allOf:
                         - allOf:
                             - type: string
                         - oneOf:
                             - type: string  # violates sch-oneOf, but not this rule
                             - type: object
                             - description: "..." # any type
                """);

        var schema = getSchemaDefinition("AllOfComplicated", openapi);
        var validationResult = ApiFunctions.findSchemaTypes(schema.getModel(), openapi);
        assertFalse(validationResult.hasConflict());
        assertEquals(1, validationResult.allowedTypes().size());
    }

    private SchemaDefinition getSchemaDefinition(String schemaIdentifier, Parser.ParserResult parserResult) {
        return parserResult.getSchemas().stream()
                .filter(def -> def.getIdentifier() != null && def.getIdentifier().equals(schemaIdentifier))
                .findAny().get();
    }

    private Parser.ParserResult parseOpenApi(String fileName) {
        var oas = new ViolationReport();
        var file = new File(Objects.requireNonNull(this.getClass().getResource("../rules/" + fileName)).getFile());
        return new Parser(file).parse(oas);
    }

    private Parser.ParserResult parseOpenApiFromString(String schemasString) { //via temp file
        var openApiContents = """
                openapi: 3.0.1
                info:
                  title: TestCase
                  version: '1.0'
                servers:
                  - url: '/api/v1'
                paths: {}
                components:
                  schemas:
                """
                + schemasString;
        var oas = new ViolationReport();
        Path tempOpenApiFile = tempDir.resolve("tempOpenApiFile.yaml");
        try {
            Files.write(tempOpenApiFile, openApiContents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Parser(tempOpenApiFile.toFile()).parse(oas);
    }
}