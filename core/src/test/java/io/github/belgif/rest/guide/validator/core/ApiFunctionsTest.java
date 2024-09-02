package io.github.belgif.rest.guide.validator.core;

import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class ApiFunctionsTest {
    @Test
    void testUrl() {
        var pattern = "^((https:\\/\\/)|\\/?)[-a-zA-Z0-9@:%._\\+~#=]{1,256}[a-zA-Z0-9()]{1,6}\\/[A-Za-z0-9]*(\\/[a-z0-9]+([A-Z]?[a-z0-9]+)*)*\\/v[0-9]+$";
        var url = "https://localhost:8080/REST/v1";
        assertTrue(url.matches(pattern));
        url = "/REST/contactData/v1";
        assertTrue(url.matches(pattern));
    }

    @Test
    void testUrlV3() {
        var pattern = "^((http://localhost/?)|(https://)|/?)[-a-zA-Z0-9@:%._\\+~#=]{1,256}[a-zA-Z0-9()]{1,6}(\\/[A-Za-z0-9]*)*(\\/[a-z0-9]+([A-Z]?[a-z0-9]+)*)*\\/v[0-9]+$";

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
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(MediaType.parseMediaType("multipart/*"));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/problem+json", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/json", mediaTypes));
        assertFalse(ApiFunctions.isMediaTypeIncluded("application/problem+xml", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/*", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/form-data", mediaTypes));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/chunked", mediaTypes));
        List<MediaType> all = new ArrayList<>();
        all.add(MediaType.ALL);
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/problem+json", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/json", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/problem+xml", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/*", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/form-data", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("multipart/chunked", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("*/*", all));
        assertTrue(ApiFunctions.isMediaTypeIncluded("application/*", all));

        assertFalse(ApiFunctions.isMediaTypeIncluded("*/*", List.of(MediaType.APPLICATION_JSON)));
        assertFalse(ApiFunctions.isMediaTypeIncluded("*/*", List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_XML, MediaType.APPLICATION_XML, MediaType.parseMediaType("multipart/*"))));

    }

    @Test
    void existsPathWithPathParamAfterTest() {
        var oas = new OpenApiViolationAggregator();
        var file = new File(this.getClass().getResource("../rules/isCollection.yaml").getFile());
        var result = new Parser(file).parse(oas);

        var defs = result.getPathDefinitions();
        var logos = defs.stream().filter(definition -> "/logos".equals(definition.getIdentifier())).findAny();
        assertTrue(logos.isPresent());
        assertTrue(ApiFunctions.existsPathWithPathParamAfter(logos.get().getIdentifier(), result));
    }

}