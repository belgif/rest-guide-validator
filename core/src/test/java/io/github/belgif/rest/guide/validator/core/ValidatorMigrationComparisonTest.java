package io.github.belgif.rest.guide.validator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.JSON;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.validateCustomizedOpenApi4j;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.validateNetworknt;
import static org.junit.jupiter.api.Assertions.*;

import io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.Observed;
import io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.State;

class ValidatorMigrationComparisonTest {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    @TempDir
    Path directory;

    private static final List<Probe> PROBES = List.of(
            probe("oneof-discriminator-overlapping-branches",
                    """
                    openapi: 3.0.3
                    info: {title: oneof-discriminator-overlapping-branches, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind, value]
                          properties:
                            kind: {type: string}
                            value: {type: string}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                    """,
                    "{kind: a, value: x}",
                    State.VALID, State.INVALID),
            probe("anyof-discriminator-selected-branch-invalid-alternate-valid",
                    """
                    openapi: 3.0.3
                    info: {title: anyof-discriminator-selected-branch-invalid-alternate-valid, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind, value]
                          properties:
                            kind: {type: string}
                            value: {type: string}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          anyOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: integer}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                    """,
                    "{kind: a, value: x}",
                    State.INVALID, State.VALID),
            probe("discriminator-explicit-reference-mapping",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-explicit-reference-mapping, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                            a: {type: string}
                            b: {type: integer}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [b]}
                            value: {type: string}
                    """,
                    "{kind: a, a: ok}",
                    State.VALID, State.VALID),
            probe("discriminator-schema-name-shorthand-confounded-control",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-schema-name-shorthand-confounded-control, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                            a: {type: string}
                            b: {type: integer}
                          discriminator:
                            propertyName: kind
                            mapping: {a: A}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [b]}
                            value: {type: string}
                    """,
                    "{kind: a, a: ok}",
                    State.INVALID, State.INVALID),
            probe("discriminator-implicit-schema-name",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-implicit-schema-name, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                            a: {type: string}
                            b: {type: integer}
                          discriminator: {propertyName: kind}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [A]}
                            value: {type: string}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [b]}
                            value: {type: string}
                    """,
                    "{kind: A, a: ok}",
                    State.VALID, State.VALID),
            probe("discriminator-missing",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-missing, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                            a: {type: string}
                            b: {type: integer}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [b]}
                            value: {type: string}
                    """,
                    "{a: ok}",
                    State.INVALID, State.INVALID),
            probe("discriminator-null",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-null, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                            a: {type: string}
                            b: {type: integer}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [b]}
                            value: {type: string}
                    """,
                    "{kind: null, a: ok}",
                    State.INVALID, State.INVALID),
            probe("discriminator-non-string",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-non-string, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                            a: {type: string}
                            b: {type: integer}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [b]}
                            value: {type: string}
                    """,
                    "{kind: 7, a: ok}",
                    State.INVALID, State.INVALID),
            probe("discriminator-unknown",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-unknown, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                            a: {type: string}
                            b: {type: integer}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [a]}
                            value: {type: string}
                        B:
                          type: object
                          required: [kind]
                          properties:
                            kind: {enum: [b]}
                            value: {type: string}
                    """,
                    "{kind: missing, a: ok}",
                    State.INVALID, State.INVALID),
            probe("discriminator-unconstrained-missing",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-unconstrained-missing, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          properties:
                            kind: {}
                            a: {type: string}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [a]
                          properties:
                            a: {type: string}
                        B:
                          type: object
                          required: [a]
                          properties:
                            a: {type: integer}
                    """,
                    "{a: ok}",
                    State.INVALID, State.VALID),
            probe("discriminator-unconstrained-null",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-unconstrained-null, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          properties:
                            kind: {}
                            a: {type: string}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [a]
                          properties:
                            a: {type: string}
                        B:
                          type: object
                          required: [a]
                          properties:
                            a: {type: integer}
                    """,
                    "{kind: null, a: ok}",
                    State.INVALID, State.VALID),
            probe("discriminator-unconstrained-non-string",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-unconstrained-non-string, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          properties:
                            kind: {}
                            a: {type: string}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [a]
                          properties:
                            a: {type: string}
                        B:
                          type: object
                          required: [a]
                          properties:
                            a: {type: integer}
                    """,
                    "{kind: 7, a: ok}",
                    State.VALID, State.VALID),
            probe("discriminator-unconstrained-unknown",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-unconstrained-unknown, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          properties:
                            kind: {}
                            a: {type: string}
                          discriminator:
                            propertyName: kind
                            mapping: {a: '#/components/schemas/A'}
                          oneOf:
                            - $ref: '#/components/schemas/A'
                            - $ref: '#/components/schemas/B'
                        A:
                          type: object
                          required: [a]
                          properties:
                            a: {type: string}
                        B:
                          type: object
                          required: [a]
                          properties:
                            a: {type: integer}
                    """,
                    "{kind: unknown, a: ok}",
                    State.INVALID, State.INVALID),
            probe("discriminator-child-allof-inherited",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-child-allof-inherited, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {$ref: '#/components/schemas/Child'}
                        Base:
                          type: object
                          properties:
                            kind: {}
                          discriminator:
                            propertyName: kind
                            mapping: {child: '#/components/schemas/Child'}
                        Child:
                          allOf:
                            - $ref: '#/components/schemas/Base'
                            - type: object
                              properties:
                                payload: {type: string}
                    """,
                    "{kind: child, payload: ok}",
                    State.VALID, State.VALID),
            probe("discriminator-child-allof-inherited-unknown",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-child-allof-inherited-unknown, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {$ref: '#/components/schemas/Child'}
                        Base:
                          type: object
                          properties:
                            kind: {}
                          discriminator:
                            propertyName: kind
                            mapping: {child: '#/components/schemas/Child'}
                        Child:
                          allOf:
                            - $ref: '#/components/schemas/Base'
                            - type: object
                              properties:
                                payload: {type: string}
                    """,
                    "{kind: unknown, payload: ok}",
                    State.INVALID, State.VALID),
            probe("discriminator-child-allof-inherited-missing",
                    """
                    openapi: 3.0.3
                    info: {title: discriminator-child-allof-inherited-missing, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {$ref: '#/components/schemas/Child'}
                        Base:
                          type: object
                          properties:
                            kind: {}
                          discriminator:
                            propertyName: kind
                            mapping: {child: '#/components/schemas/Child'}
                        Child:
                          allOf:
                            - $ref: '#/components/schemas/Base'
                            - type: object
                              properties:
                                payload: {type: string}
                    """,
                    "{payload: ok}",
                    State.INVALID, State.VALID),
            probe("nested-unrelated-discriminator",
                    """
                    openapi: 3.0.3
                    info: {title: nested-unrelated-discriminator, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          allOf:
                            - type: object
                              properties:
                                nested:
                                  type: object
                                  properties:
                                    kind: {type: string}
                                  discriminator:
                                    propertyName: kind
                    """,
                    "{nested: {kind: x}}",
                    State.VALID, State.VALID),
            probe("nullable-string-null",
                    """
                    openapi: 3.0.3
                    info: {title: nullable-string-null, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, nullable: true}
                    """,
                    "null",
                    State.VALID, State.VALID),
            probe("nullable-enum-excludes-null",
                    """
                    openapi: 3.0.3
                    info: {title: nullable-enum-excludes-null, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, nullable: true, enum: [x]}
                    """,
                    "null",
                    State.INVALID, State.VALID),
            probe("nullable-oneof-type-less-composition",
                    """
                    openapi: 3.0.3
                    info: {title: nullable-oneof-type-less-composition, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          nullable: true
                          oneOf:
                            - type: string
                            - type: integer
                    """,
                    "null",
                    State.INVALID, State.VALID),
            probe("nullable-allof-composition",
                    """
                    openapi: 3.0.3
                    info: {title: nullable-allof-composition, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          nullable: true
                          allOf:
                            - type: string
                    """,
                    "null",
                    State.INVALID, State.VALID),
            probe("type-less-schema-null",
                    """
                    openapi: 3.0.3
                    info: {title: type-less-schema-null, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {description: 'no type constraint'}
                    """,
                    "null",
                    State.INVALID, State.VALID),
            probe("required-property-present-null",
                    """
                    openapi: 3.0.3
                    info: {title: required-property-present-null, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: object, required: [value]}
                    """,
                    "{value: null}",
                    State.VALID, State.VALID),
            probe("required-property-absent",
                    """
                    openapi: 3.0.3
                    info: {title: required-property-absent, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: object, required: [value]}
                    """,
                    "{}",
                    State.INVALID, State.INVALID),
            probe("int32-upper-bound",
                    """
                    openapi: 3.0.3
                    info: {title: int32-upper-bound, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: integer, format: int32}
                    """,
                    "2147483648",
                    State.INVALID, State.VALID),
            probe("int32-lower-bound",
                    """
                    openapi: 3.0.3
                    info: {title: int32-lower-bound, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: integer, format: int32}
                    """,
                    "-2147483649",
                    State.INVALID, State.VALID),
            probe("int64-upper-bound",
                    """
                    openapi: 3.0.3
                    info: {title: int64-upper-bound, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: integer, format: int64}
                    """,
                    "9223372036854775808",
                    State.INVALID, State.VALID),
            probe("byte-valid-base64",
                    """
                    openapi: 3.0.3
                    info: {title: byte-valid-base64, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, format: byte}
                    """,
                    "'SGVsbG8='",
                    State.VALID, State.VALID),
            probe("byte-invalid-base64",
                    """
                    openapi: 3.0.3
                    info: {title: byte-invalid-base64, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, format: byte}
                    """,
                    "'not base64!'",
                    State.INVALID, State.VALID),
            probe("date-valid",
                    """
                    openapi: 3.0.3
                    info: {title: date-valid, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, format: date}
                    """,
                    "'2024-02-29'",
                    State.VALID, State.VALID),
            probe("date-invalid",
                    """
                    openapi: 3.0.3
                    info: {title: date-invalid, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, format: date}
                    """,
                    "'2023-02-29'",
                    State.VALID, State.INVALID),
            probe("date-time-invalid",
                    """
                    openapi: 3.0.3
                    info: {title: date-time-invalid, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, format: date-time}
                    """,
                    "'2024-01-01 12:00:00'",
                    State.INVALID, State.INVALID),
            probe("uri-relative-value",
                    """
                    openapi: 3.0.3
                    info: {title: uri-relative-value, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, format: uri}
                    """,
                    "'./documents/123'",
                    State.INVALID, State.INVALID),
            probe("uri-reference-relative-value",
                    """
                    openapi: 3.0.3
                    info: {title: uri-reference-relative-value, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: string, format: uri-reference}
                    """,
                    "'./documents/123'",
                    State.VALID, State.VALID),
            probe("exclusive-minimum-equality",
                    """
                    openapi: 3.0.3
                    info: {title: exclusive-minimum-equality, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: number, minimum: 1, exclusiveMinimum: true}
                    """,
                    "1",
                    State.INVALID, State.INVALID),
            probe("exclusive-maximum-equality",
                    """
                    openapi: 3.0.3
                    info: {title: exclusive-maximum-equality, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: number, maximum: 1, exclusiveMaximum: true}
                    """,
                    "1",
                    State.INVALID, State.INVALID),
            probe("additional-properties-with-allof",
                    """
                    openapi: 3.0.3
                    info: {title: additional-properties-with-allof, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          allOf:
                            - type: object
                              properties:
                                a: {type: string}
                            - type: object
                              properties:
                                b: {type: string}
                          additionalProperties: false
                    """,
                    "{a: x, b: y}",
                    State.INVALID, State.INVALID),
            probe("read-only-context-neutral",
                    """
                    openapi: 3.0.3
                    info: {title: read-only-context-neutral, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: object, required: [value], properties: {value: {type: string, readOnly: true}}}
                    """,
                    "{value: present}",
                    State.VALID, State.VALID),
            probe("write-only-context-neutral",
                    """
                    openapi: 3.0.3
                    info: {title: write-only-context-neutral, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: object, properties: {value: {type: string, writeOnly: true}}}
                    """,
                    "{value: secret}",
                    State.VALID, State.VALID),
            probe("ref-property-name-literal-dollar-ref",
                    """
                    openapi: 3.0.3
                    info: {title: ref-property-name-literal-dollar-ref, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {type: object, properties: {$ref: {type: string}}, required: [$ref]}
                    """,
                    "{$ref: literal}",
                    State.VALID, State.VALID),
            probe("json-pointer-escaped-component",
                    """
                    openapi: 3.0.3
                    info: {title: json-pointer-escaped-component, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {$ref: '#/components/schemas/A~1B~0C'}
                        'A/B~C': {type: string}
                    """,
                    "'resolved'",
                    State.VALID, State.VALID),
            probe("allof-style-discriminator",
                    """
                    openapi: 3.0.3
                    info: {title: allof-style-discriminator, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                            payload: {}
                          discriminator:
                            propertyName: kind
                            mapping: {child: '#/components/schemas/Child'}
                        Child:
                          type: object
                          required: [kind, payload]
                          allOf:
                            - $ref: '#/components/schemas/Target'
                          properties:
                            kind: {enum: [child]}
                            payload: {type: string}
                    """,
                    "{kind: unknownchild, payload: 123}",
                    State.VALID, State.VALID)
    );

    private static List<Probe> customizationProbes() {
        List<Probe> probes = new ArrayList<>();
        probes.addAll(List.of(
                formatProbe("uri-absolute", "uri", "https://example.test/a", State.VALID, State.VALID),
                formatProbe("uri-network-path-not-absolute", "uri", "//example.test/a", State.INVALID, State.INVALID),
                formatProbe("uri-malformed-percent-escape", "uri", "https://example.test/%ZZ", State.INVALID, State.INVALID),
                formatProbe("uri-non-ascii-current-behavior", "uri", "https://example.test/caf\u00e9", State.VALID, State.INVALID),
                formatProbe("uri-percent-encoded-non-ascii", "uri", "https://example.test/caf%C3%A9", State.VALID, State.VALID),
                formatProbe("uri-reference-empty", "uri-reference", "", State.VALID, State.VALID),
                formatProbe("uri-reference-fragment", "uri-reference", "#part", State.VALID, State.VALID),
                formatProbe("uri-reference-non-ascii", "uri-reference", "/caf\u00e9", State.INVALID, State.INVALID),
                formatProbe("uri-reference-percent-encoded-non-ascii", "uri-reference", "/caf%C3%A9", State.VALID, State.VALID),
                formatProbe("uri-reference-malformed-percent-escape", "uri-reference", "/%ZZ", State.INVALID, State.INVALID),
                formatProbe("uri-reference-raw-query-brackets-current-behavior", "uri-reference", "?x=[a]", State.VALID, State.INVALID),
                formatProbe("uri-reference-encoded-query-brackets", "uri-reference", "?x=%5Ba%5D", State.VALID, State.VALID),
                formatProbe("uri-raw-query-brackets-current-behavior", "uri", "https://example.test/?x=[a]", State.VALID, State.INVALID),
                formatProbe("uri-encoded-query-brackets", "uri", "https://example.test/?x=%5Ba%5D", State.VALID, State.VALID),
                formatProbe("uriref-alias-relative", "uriref", "./a", State.VALID, State.VALID),
                formatProbe("uriref-alias-malformed-percent-escape", "uriref", "/%ZZ", State.INVALID, State.VALID)
        ));

        String nestedReference = """
                openapi: 3.0.3
                info: {title: nested-reference-discriminator, version: '1'}
                paths: {}
                components:
                  schemas:
                    Target:
                      type: object
                      allOf:
                        - type: object
                          required: [nested]
                          properties:
                            nested: {$ref: '#/components/schemas/Base'}
                    Base:
                      type: object
                      required: [kind]
                      properties:
                        kind: {type: string}
                      discriminator:
                        propertyName: kind
                        mapping: {child: '#/components/schemas/Child'}
                      oneOf:
                        - $ref: '#/components/schemas/Child'
                    Child:
                      type: object
                      required: [kind, payload]
                      properties:
                        kind: {enum: [child]}
                        payload: {type: string}
                """;
        probes.add(probe("nested-ref-discriminator-no-root-kind", nestedReference,
                "{nested: {kind: child, payload: ok}}", State.VALID, State.VALID));
        probes.add(probe("nested-ref-discriminator-invalid-payload", nestedReference,
                "{nested: {kind: child, payload: 42}}", State.INVALID, State.INVALID));
        probes.add(probe("nested-ref-discriminator-missing-nested-kind", nestedReference,
                "{nested: {payload: ok}}", State.INVALID, State.INVALID));
        probes.add(probe("nested-ref-discriminator-root-kind-does-not-replace-nested-kind", nestedReference,
                "{kind: child, nested: {payload: ok}}", State.INVALID, State.INVALID));

        for (String mapping : List.of("Target", "#/components/schemas/Target")) {
            String spelling = mapping.equals("Target") ? "schema-name" : "reference";
            String document = """
                    openapi: 3.0.3
                    info: {title: inherited-discriminator-mapping, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Base:
                          type: object
                          required: [kind]
                          properties:
                            kind: {type: string}
                          discriminator:
                            propertyName: kind
                            mapping: {child: '%s'}
                        Target:
                          allOf:
                            - $ref: '#/components/schemas/Base'
                            - type: object
                              required: [payload]
                              properties:
                                payload: {type: string}
                    """.formatted(mapping);
            probes.add(probe("discriminator-mapping-" + spelling + "-valid", document,
                    "{kind: child, payload: ok}", State.VALID, State.VALID));
            probes.add(probe("discriminator-mapping-" + spelling + "-wrong-payload-type", document,
                    "{kind: child, payload: 42}", State.INVALID, State.INVALID));
            probes.add(probe("discriminator-mapping-" + spelling + "-missing-payload", document,
                    "{kind: child}", State.INVALID, State.INVALID));
        }

        for (String mapping : List.of("Missing", "#/components/schemas/Missing")) {
            String spelling = mapping.equals("Missing") ? "schema-name" : "reference";
            probes.add(probe("unresolved-discriminator-mapping-" + spelling, """
                    openapi: 3.0.3
                    info: {title: unresolved-discriminator-mapping, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          discriminator:
                            propertyName: kind
                            mapping: {missing: '%s'}
                          oneOf:
                            - $ref: '#/components/schemas/Child'
                        Child:
                          type: object
                          required: [kind, payload]
                          properties:
                            kind: {enum: [missing]}
                            payload: {type: string}
                    """.formatted(mapping),
                    "{kind: missing, payload: ok}", State.EXECUTION_FAILURE, State.INVALID));
        }

        String external = """
                openapi: 3.0.3
                info: {title: external-schema, version: '1'}
                paths: {}
                components:
                  schemas:
                    Thing: {type: string}
                    Child:
                      type: object
                      required: [kind, payload]
                      properties:
                        kind: {enum: [child]}
                        payload: {type: string}
                """;
        for (String separator : List.of("", "/")) {
            String suffix = separator.isEmpty() ? "ordinary" : "trailing-slash";
            // Networknt does not resolve filename.yaml/#/... to filename.yaml.
            State networkntValid = separator.isEmpty() ? State.VALID : State.EXECUTION_FAILURE;
            State networkntInvalid = separator.isEmpty() ? State.INVALID : State.EXECUTION_FAILURE;
            String document = """
                    openapi: 3.0.3
                    info: {title: external-reference-identity, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target: {$ref: 'external.yaml%s#/components/schemas/Thing'}
                        Thing: {type: integer}
                    """.formatted(separator);
            Map<String, String> references = Map.of("external.yaml", external);
            probes.add(probe("external-ref-" + suffix + "-uses-external-schema", document,
                    "'external value'", State.VALID, networkntValid, references));
            probes.add(probe("external-ref-" + suffix + "-rejects-root-only-value", document,
                    "42", State.INVALID, networkntInvalid, references));

            String mappingDocument = """
                    openapi: 3.0.3
                    info: {title: external-discriminator-mapping, version: '1'}
                    paths: {}
                    components:
                      schemas:
                        Target:
                          type: object
                          discriminator:
                            propertyName: kind
                            mapping: {child: 'external.yaml%s#/components/schemas/Child'}
                          oneOf:
                            - $ref: 'external.yaml%s#/components/schemas/Child'
                        Child:
                          type: object
                          required: [kind, payload]
                          properties:
                            kind: {enum: [child]}
                            payload: {type: integer}
                    """.formatted(separator, separator);
            probes.add(probe("external-discriminator-" + suffix + "-valid", mappingDocument,
                    "{kind: child, payload: ok}", State.VALID, networkntValid, references));
            probes.add(probe("external-discriminator-" + suffix + "-rejects-root-only-value", mappingDocument,
                    "{kind: child, payload: 42}", State.INVALID, networkntInvalid, references));
        }
        return probes;
    }

    private static Probe formatProbe(String id, String format, String value,
                                     State expectedOpenApi4j, State expectedNetworknt) {
        return probe(id, """
                openapi: 3.0.3
                info: {title: customized-format, version: '1'}
                paths: {}
                components:
                  schemas:
                    Target: {type: string, format: '%s'}
                """.formatted(format), JSON.getNodeFactory().textNode(value).toString(),
                expectedOpenApi4j, expectedNetworknt);
    }

    @TestFactory
    List<DynamicTest> targetedOpenApi30Matrix() {
        List<Probe> probes = Stream.concat(PROBES.stream(), customizationProbes().stream()).toList();
        assertEquals(probes.size(), probes.stream().map(Probe::id).distinct().count(), "Unique probe IDs");
        long oldRejectsNewAccepts = probes.stream()
                .filter(probe -> probe.expectedOpenApi4j() == State.INVALID
                        && probe.expectedNetworknt() == State.VALID)
                .count();
        long oldAcceptsNewRejects = probes.stream()
                .filter(probe -> probe.expectedOpenApi4j() == State.VALID
                        && probe.expectedNetworknt() == State.INVALID)
                .count();
        long same = probes.stream()
                .filter(probe -> probe.expectedOpenApi4j() == probe.expectedNetworknt()).count();
        long executionDifferences = probes.size() - same - oldRejectsNewAccepts - oldAcceptsNewRejects;
        System.out.printf("SUMMARY|matrix=%d|same=%d|production-openapi4j-invalid-networknt-valid=%d|production-openapi4j-valid-networknt-invalid=%d|execution-differences=%d%n",
                probes.size(), same, oldRejectsNewAccepts, oldAcceptsNewRejects, executionDifferences);

        return probes.stream()
                .map(probe -> DynamicTest.dynamicTest(probe.id(), () -> compare(probe)))
                .toList();
    }

    @Test
    void unresolvedReferencesAreExecutionFailuresNotInvalidInstances() throws IOException {
        compare(probe("unresolved-reference", """
                openapi: 3.0.3
                info: {title: unresolved-reference, version: '1'}
                paths: {}
                components:
                  schemas:
                    Target: {$ref: '#/components/schemas/Missing'}
                """, "'anything'", State.EXECUTION_FAILURE, State.EXECUTION_FAILURE));
    }

    private void compare(Probe probe) throws IOException {
        Path probeDirectory = Files.createDirectory(directory.resolve(probe.id()));
        Path root = probeDirectory.resolve("openapi.yaml");
        Files.writeString(root, JSON.writeValueAsString(probe.document()));
        String documentLocation = root.toUri().toString();
        Map<String, String> documents = new HashMap<>();
        documents.put(documentLocation, JSON.writeValueAsString(probe.document()));
        for (Map.Entry<String, JsonNode> entry : probe.referencedDocuments().entrySet()) {
            Path file = probeDirectory.resolve(entry.getKey());
            String content = JSON.writeValueAsString(entry.getValue());
            Files.writeString(file, content);
            documents.put(file.toUri().toString(), content);
        }
        JsonNode schema = probe.document().at("/components/schemas/Target");
        assertFalse(schema.isMissingNode(), "Probe document has no components.schemas.Target: " + probe.id());
        String schemaLocation = documentLocation + "#/components/schemas/Target";

        Observed old = validateCustomizedOpenApi4j(root.toFile(), probe.instance());
        Observed newer = validateNetworknt(schema, probe.instance(), schemaLocation, documents);

        System.out.printf("MATRIX|%s|production-openapi4j=%s|networknt=%s%n",
                probe.id(), old.csv(), newer.csv());
        if (Set.of("anyof-discriminator-selected-branch-invalid-alternate-valid",
                "discriminator-schema-name-shorthand-confounded-control", "discriminator-implicit-schema-name",
                "required-property-absent", "exclusive-minimum-equality", "uri-relative-value",
                "additional-properties-with-allof", "nullable-enum-excludes-null").contains(probe.id())) {
            System.out.printf("DETAIL|%s|production-openapi4j=%s|networknt=%s|networknt-keywords-locations=%s%n",
                    probe.id(), old.message(), newer.message(), newer.keywordsAndLocations());
        }
        assertEquals(probe.expectedOpenApi4j(), old.state(),
                "production openapi4j outcome for " + probe.id() + ": " + old.message());
        assertEquals(probe.expectedNetworknt(), newer.state(),
                "networknt outcome for " + probe.id() + ": " + newer.message());
    }

    private static Probe probe(String id, String document, String instance,
                               State expectedOpenApi4j, State expectedNetworknt) {
        return probe(id, document, instance, expectedOpenApi4j, expectedNetworknt, Map.of());
    }

    private static Probe probe(String id, String document, String instance,
                               State expectedOpenApi4j, State expectedNetworknt,
                               Map<String, String> referencedDocuments) {
        try {
            Map<String, JsonNode> parsedDocuments = new HashMap<>();
            for (Map.Entry<String, String> entry : referencedDocuments.entrySet()) {
                parsedDocuments.put(entry.getKey(), YAML.readTree(entry.getValue()));
            }
            return new Probe(id, YAML.readTree(document), YAML.readTree(instance),
                    expectedOpenApi4j, expectedNetworknt, Map.copyOf(parsedDocuments));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private record Probe(String id, JsonNode document, JsonNode instance,
                         State expectedOpenApi4j, State expectedNetworknt,
                         Map<String, JsonNode> referencedDocuments) {
    }
}
