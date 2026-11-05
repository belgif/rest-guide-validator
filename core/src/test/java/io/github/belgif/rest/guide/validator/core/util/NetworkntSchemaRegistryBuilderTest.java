package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkntSchemaRegistryBuilderTest {
    // When reading example values, Jackson deserializes JSON numbers into Double by default, which can cause precision loss when preparing our test values to pass to networknt. Use BigDecimal to preserve precision.
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
    // dummy URI to assign to test schema
    private static final String SCHEMA_URI = "https://example.test/schema";

    @Test
    void int32AndInt64ValidateIntegerRanges() throws Exception {
        assertValid("int32", "-2147483648");
        assertValid("int32", "2147483647");
        assertInvalid("int32", "2147483648");
        assertInvalid("int32", "-2147483649");
        assertValid("int64", "-9223372036854775808");
        assertValid("int64", "9223372036854775807");
        assertInvalid("int64", "9223372036854775808");
        assertInvalid("int64", "-9223372036854775809");
    }

    @Test
    void floatAndDoublePreserveRepresentablePrecision() throws Exception {
        assertValid("float", "0.1");
        assertInvalid("float", "0.1000000001");
        assertInvalid("float", "3.5e38");

        assertValid("double", "0.1");
        assertInvalid("double", "9007199254740993.1");
        assertInvalid("double", "1e309");
    }

    @Test
    void byteAcceptsStandardPaddedBase64IncludingEmptyStrings() throws Exception {
        assertValid("byte", "\"\"");
        assertValid("byte", "\"AA==\"");
        assertValid("byte", "\"AAA=\"");
        assertValid("byte", "\"AAAA\"");
        assertInvalid("byte", "\"A\"");
        assertInvalid("byte", "\"AAA\"");
        assertInvalid("byte", "\"A===\"");
        assertInvalid("byte", "\"AA=A\"");
        assertInvalid("byte", "\"AA===\"");
        assertInvalid("byte", "\"AA-_\"");
    }

    @Test
    void customFormatErrorsHaveActionableBuiltInMessages() throws Exception {
        Error error = validate("int32", "2147483648").get(0);

        assertTrue(error.getMessage().contains("int32"));
    }

    private static void assertValid(String format, String instance) throws Exception {
        assertTrue(validate(format, instance).isEmpty(), format + " should accept " + instance);
    }

    private static void assertInvalid(String format, String instance) throws Exception {
        assertFalse(validate(format, instance).isEmpty(), format + " should reject " + instance);
    }

    private static List<Error> validate(String format, String value) throws Exception {
        JsonNode schemaNode = JSON.readTree("{\"type\":\"" + ("byte".equals(format) ? "string" : "number")
                + "\",\"format\":\"" + format + "\"}");
        JsonNode instance = JSON.readTree(value);
        SchemaRegistry registry = NetworkntSchemaRegistryBuilder.createRegistry(Map.of());
        Schema schema = registry.getSchema(SchemaLocation.of(SCHEMA_URI), schemaNode);
        return schema.validate(instance);
    }
}
