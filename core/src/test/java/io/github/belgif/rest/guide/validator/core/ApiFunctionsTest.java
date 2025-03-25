package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.model.OperationDefinition;
import io.github.belgif.rest.guide.validator.core.model.helper.MediaType;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class ApiFunctionsTest {
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
        var oas = new ViolationReport();
        var file = new File(Objects.requireNonNull(this.getClass().getResource("../rules/isCollection.yaml")).getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getPathDefinitions();
        var logos = defs.stream().filter(definition -> "/logos".equals(definition.getIdentifier())).findAny();
        assertTrue(logos.isPresent());
        assertTrue(ApiFunctions.existsPathWithPathParamAfter(logos.get().getIdentifier(), result));
    }

    @Test
    void findCallingOperationsTest() {
        var oas = new ViolationReport();
        var file = new File(Objects.requireNonNull(this.getClass().getResource("../rules/findCallingOperations.yaml")).getFile());
        var result = new Parser(file).parse(oas);

        var innerSchema = result.getSchemas().stream().filter(definition -> definition.getIdentifier() != null && definition.getIdentifier().equals("MyChildObject")).findAny().get();
        Set<OperationDefinition> operations = ApiFunctions.findOperationsUsingDefinition(innerSchema, false);
        assertEquals(3, operations.size());
        operations = ApiFunctions.findOperationsUsingDefinition(innerSchema, true);
        assertEquals(1, operations.size());

        var paramSchema = result.getSchemas().stream().filter(definition -> definition.getIdentifier() != null && definition.getIdentifier().equals("MyParamObject")).findAny().get();
        operations = ApiFunctions.findOperationsUsingDefinition(paramSchema, false);
        assertEquals(1, operations.size());
        var operation = operations.iterator().next();
        assertEquals("myPath", operation.getModel().getOperationId());
    }

}