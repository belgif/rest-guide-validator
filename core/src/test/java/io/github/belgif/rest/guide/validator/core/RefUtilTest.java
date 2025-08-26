package io.github.belgif.rest.guide.validator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.belgif.rest.guide.validator.core.parser.RefUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefUtilTest {

    JsonNode document;

    @BeforeEach
    void setUp() throws IOException {
        if (document == null) {
            document = getDocumentNode();
        }
    }

    @Test
    void testRefIsCorrectLocation() {
        JsonNode refNode = document.findParent("AnExample").get("AnExample");
        Optional<Boolean> validLocation = RefUtil.isInReferenceObjectLocation(refNode, document);
        assertTrue(validLocation.isPresent());
        assertTrue(validLocation.get());
    }

    @Test
    void testRefIsIncorrectLocationProperties() {
        JsonNode refNode = document.findParent("properties").get("properties");
        Optional<Boolean> validLocation = RefUtil.isInReferenceObjectLocation(refNode, document);
        assertTrue(validLocation.isPresent());
        assertFalse(validLocation.get());
    }

    @Test
    void testRefIsIncorrectLocationExampleComponent() {
        JsonNode refNode = document.findParent("ThisIsQuiteAnExample").get("ThisIsQuiteAnExample").get("value");
        Optional<Boolean> validLocation = RefUtil.isInReferenceObjectLocation(refNode, document);
        assertTrue(validLocation.isPresent());
        assertFalse(validLocation.get());
    }

    @Test
    void testRefIsIncorrectLocationExampleInline() {
        JsonNode refNode = document.findParent("oneOf").get("oneOf").get(0).get("example");
        Optional<Boolean> validLocation = RefUtil.isInReferenceObjectLocation(refNode, document);
        assertTrue(validLocation.isPresent());
        assertFalse(validLocation.get());
    }

    @Test
    void testRefNotInDocument() {
        JsonNode refNode = TextNode.valueOf("#/does/not/exist");
        Optional<Boolean> validLocation = RefUtil.isInReferenceObjectLocation(refNode, document);
        assertFalse(validLocation.isPresent());
    }

    @Test
    void testRefOnRoot() {
        Map<String, String> root = Map.of("$ref", "#/does/not/matter");
        JsonNode refNode = new ObjectMapper().valueToTree(root);
        Optional<Boolean> validLocation = RefUtil.isInReferenceObjectLocation(refNode, refNode);
        assertTrue(validLocation.isPresent());
        assertFalse(validLocation.get());
    }

    private JsonNode getDocumentNode() throws IOException {
        var file = new File(Objects.requireNonNull(this.getClass().getResource("../rules/refsInLocations.yaml")).getFile());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        return mapper.readTree(file);
    }
}
