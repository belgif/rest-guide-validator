package io.github.belgif.rest.guide.validator.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaContext;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.format.Format;
import com.networknt.schema.resource.MapResourceLoader;
import com.networknt.schema.resource.SchemaLoader;
import com.networknt.schema.utils.JsonType;
import com.networknt.schema.utils.TypeFactory;

import java.math.BigDecimal;
import java.util.Map;

final class NetworkntSchemaRegistryBuilder {
    private static final Dialect OPEN_API_30_DIALECT = Dialect.builder(Dialects.getOpenApi30())
            .format(new Int32Format())
            .format(new Int64Format())
            .format(new FloatFormat())
            .format(new DoubleFormat())
            .format(new ByteFormat())
            .build();

    private NetworkntSchemaRegistryBuilder() {
    }

    static SchemaRegistry createRegistry(Map<String, String> documents) {
        return SchemaRegistry.withDialect(OPEN_API_30_DIALECT, builder -> {
            builder.schemaLoader(new SchemaLoader(new MapResourceLoader(documents)));
            builder.schemaRegistryConfig(SchemaRegistryConfig.builder().build());
        });
    }

    /*
     * Format validation implementations to add support for OpenAPI 3.0 formats not part of the JSON Schema standard or provided by the networknt implementation.
     *
     * Adapted and modified for networknt's Format API from Atlassian openapi-request-validator (Apache-2.0):
     * https://github.com/atlassian/openapi-request-validator/tree/master/openapi-request-validator-core/src/main/java/com/atlassian/oai/validator/schema/format
     * Copyright @ 2016 Atlassian Pty Ltd.
     * License: https://www.apache.org/licenses/LICENSE-2.0
     */
    private abstract static class OpenAPI3Format implements Format {
        @Override
        public String getMessageKey() {
            return "format";
        }
    }

    private static final class Int32Format extends OpenAPI3Format {
        @Override
        public String getName() {
            return "int32";
        }

        @Override
        public boolean matches(ExecutionContext executionContext, SchemaContext schemaContext, JsonNode value) {
            return TypeFactory.getValueNodeType(value, schemaContext.getSchemaRegistryConfig()) != JsonType.INTEGER
                    || value.canConvertToInt();
        }
    }

    private static final class Int64Format extends OpenAPI3Format {
        @Override
        public String getName() {
            return "int64";
        }

        @Override
        public boolean matches(ExecutionContext executionContext, SchemaContext schemaContext, JsonNode value) {
            return TypeFactory.getValueNodeType(value, schemaContext.getSchemaRegistryConfig()) != JsonType.INTEGER
                    || value.canConvertToLong();
        }
    }

    private static final class FloatFormat extends OpenAPI3Format {
        @Override
        public String getName() {
            return "float";
        }

        @Override
        public boolean matches(ExecutionContext executionContext, SchemaContext schemaContext, JsonNode value) {
            if (TypeFactory.getValueNodeType(value, schemaContext.getSchemaRegistryConfig()) != JsonType.NUMBER) {
                return true;
            }
            NumericNode numericValue = (NumericNode) value;
            if (numericValue.isNaN()) {
                return false;
            }
            return String.valueOf(numericValue.decimalValue())
                    .equals(String.valueOf(numericValue.floatValue()));
        }
    }

    private static final class DoubleFormat extends OpenAPI3Format {
        @Override
        public String getName() {
            return "double";
        }

        @Override
        public boolean matches(ExecutionContext executionContext, SchemaContext schemaContext, JsonNode value) {
            if (TypeFactory.getValueNodeType(value, schemaContext.getSchemaRegistryConfig()) != JsonType.NUMBER) {
                return true;
            }
            NumericNode numericValue = (NumericNode) value;
            if (numericValue.isNaN()) {
                return false;
            }
            double convertedValue = numericValue.doubleValue();
            if (!Double.isFinite(convertedValue)) {
                return false;
            }
            BigDecimal original = numericValue.decimalValue();
            BigDecimal converted = BigDecimal.valueOf(convertedValue);
            return original.compareTo(converted) == 0;
        }
    }

    private static final class ByteFormat extends OpenAPI3Format {
        @Override
        public String getName() {
            return "byte";
        }

        @Override
        public boolean matches(ExecutionContext executionContext, String value) {
            int length = value.length();
            if (length == 0) {
                return true;
            }
            if (length % 4 != 0) {
                return false;
            }

            int padding = value.endsWith("==") ? 2 : value.endsWith("=") ? 1 : 0;
            int contentLength = length - padding;
            if ((padding == 1 && contentLength % 4 != 3)
                    || (padding == 2 && contentLength % 4 != 2)) {
                return false;
            }
            for (int i = 0; i < contentLength; i++) {
                char character = value.charAt(i);
                if (!isBase64Character(character)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isBase64Character(char character) {
            return character >= 'A' && character <= 'Z'
                    || character >= 'a' && character <= 'z'
                    || character >= '0' && character <= '9'
                    || character == '+'
                    || character == '/';
        }
    }
}
