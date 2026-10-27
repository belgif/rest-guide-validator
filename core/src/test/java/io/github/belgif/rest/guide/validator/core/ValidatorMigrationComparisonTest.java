package io.github.belgif.rest.guide.validator.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.JSON;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.validateCustomizedOpenApi4j;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.validateNetworknt;
import static io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.validateOpenApi4j;
import static org.junit.jupiter.api.Assertions.*;

import io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.Observed;
import io.github.belgif.rest.guide.validator.core.ValidatorMigrationTestSupport.State;

class ValidatorMigrationComparisonTest {
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final String TEST_BASE = "file:///networknt-migration/";

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
                    State.VALID, State.INVALID, State.VALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.INVALID, State.VALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.VALID, State.INVALID, State.VALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.INVALID, State.INVALID, State.INVALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.VALID, State.VALID, State.VALID),
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
                    State.VALID, State.VALID, State.VALID)
    );

    @TestFactory
    List<DynamicTest> targetedOpenApi30Matrix() {
        long oldRejectsNewAccepts = PROBES.stream()
                .filter(probe -> probe.expectedOpenApi4j() == State.INVALID
                        && probe.expectedNetworknt() == State.VALID)
                .count();
        long oldAcceptsNewRejects = PROBES.stream()
                .filter(probe -> probe.expectedOpenApi4j() == State.VALID
                        && probe.expectedNetworknt() == State.INVALID)
                .count();
        assertEquals(13, oldRejectsNewAccepts);
        assertEquals(2, oldAcceptsNewRejects);
        System.out.printf("SUMMARY|matrix=%d|same=%d|openapi4j-invalid-networknt-valid=%d|openapi4j-valid-networknt-invalid=%d%n",
                PROBES.size(), PROBES.size() - oldRejectsNewAccepts - oldAcceptsNewRejects,
                oldRejectsNewAccepts, oldAcceptsNewRejects);

        return PROBES.stream()
                .map(probe -> DynamicTest.dynamicTest(probe.id(), () -> compare(probe)))
                .toList();
    }

    @Test
    void unresolvedReferencesAreExecutionFailuresNotInvalidInstances() throws IOException {
        JsonNode document = YAML.readTree("""
                openapi: 3.0.3
                info: {title: unresolved-reference, version: '1'}
                paths: {}
                components:
                  schemas:
                    Target: {$ref: '#/components/schemas/Missing'}
                """);
        JsonNode instance = YAML.readTree("'anything'");
        String documentLocation = TEST_BASE + "unresolved.yaml";
        Map<String, String> documents = Map.of(documentLocation, JSON.writeValueAsString(document));
        JsonNode schema = document.at("/components/schemas/Target");

        Observed old = validateOpenApi4j(schema, instance, document, documentLocation);
        Observed newer = validateNetworknt(schema, instance,
                documentLocation + "#/components/schemas/Target", documents);
        Observed customized = validateCustomizedOpenApi4j(document, instance);

        assertEquals(State.EXECUTION_FAILURE, old.state(), old.message());
        assertEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
        assertEquals(State.EXECUTION_FAILURE, customized.state(), customized.message());
    }

    private static void compare(Probe probe) throws IOException {
        String documentLocation = TEST_BASE + probe.id() + ".yaml";
        Map<String, String> documents = Map.of(documentLocation, JSON.writeValueAsString(probe.document()));
        JsonNode schema = probe.document().at("/components/schemas/Target");
        assertFalse(schema.isMissingNode(), "Probe document has no components.schemas.Target: " + probe.id());
        String schemaLocation = documentLocation + "#/components/schemas/Target";

        Observed old = validateOpenApi4j(schema, probe.instance(), probe.document(), documentLocation);
        Observed newer = validateNetworknt(schema, probe.instance(), schemaLocation, documents);
        Observed customized = validateCustomizedOpenApi4j(probe.document(), probe.instance());

        System.out.printf("MATRIX|%s|openapi4j=%s|networknt=%s|customized-openapi4j=%s%n",
                probe.id(), old.csv(), newer.csv(), customized.csv());
        if (Set.of("anyof-discriminator-selected-branch-invalid-alternate-valid",
                "discriminator-schema-name-shorthand-confounded-control", "discriminator-implicit-schema-name",
                "required-property-absent", "exclusive-minimum-equality", "uri-relative-value",
                "additional-properties-with-allof", "nullable-enum-excludes-null").contains(probe.id())) {
            System.out.printf("DETAIL|%s|openapi4j=%s|networknt=%s|networknt-keywords-locations=%s%n",
                    probe.id(), old.message(), newer.message(), newer.keywordsAndLocations());
        }
        assertNotEquals(State.EXECUTION_FAILURE, old.state(), old.message());
        assertNotEquals(State.EXECUTION_FAILURE, newer.state(), newer.message());
        assertNotEquals(State.EXECUTION_FAILURE, customized.state(), customized.message());
        assertEquals(probe.expectedOpenApi4j(), old.state(),
                "openapi4j outcome for " + probe.id() + ": " + old.message());
        assertEquals(probe.expectedNetworknt(), newer.state(),
                "networknt outcome for " + probe.id() + ": " + newer.message());
        assertEquals(probe.expectedCustomizedOpenApi4j(), customized.state(),
                "customized openapi4j outcome for " + probe.id() + ": " + customized.message());
    }

    private static Probe probe(String id, String document, String instance,
                               State expectedOpenApi4j, State expectedNetworknt,
                               State expectedCustomizedOpenApi4j) {
        try {
            return new Probe(id, YAML.readTree(document), YAML.readTree(instance),
                    expectedOpenApi4j, expectedNetworknt, expectedCustomizedOpenApi4j);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private record Probe(String id, JsonNode document, JsonNode instance,
                         State expectedOpenApi4j, State expectedNetworknt,
                         State expectedCustomizedOpenApi4j) {
    }
}
