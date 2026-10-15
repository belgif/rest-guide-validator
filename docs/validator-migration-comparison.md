# Schema validator migration comparison

## Scope and method

This is a bounded runtime comparison, not approval to replace or configure the
production validator. It compares this repository's patched openapi4j 1.0.7
against test-scoped `com.networknt:json-schema-validator:2.0.8`, using
`Dialects.getOpenApi30()` and library defaults. Production code, dependencies,
and validation settings are unchanged. Jackson remains managed by the existing
2.22.3 BOM.

The test adapter is
[`ValidatorMigrationComparisonTest.java`](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java).
Parser-backed tests supply only full documents already loaded by the
repository parser; synthetic test documents are in memory. The adapter does
not fetch files or URLs. Resolution and other execution failures are classified
as `EXECUTION_FAILURE`, not as invalid instances. The unresolved-reference
control asserts execution failure in both implementations. No fail-fast,
strictness, readOnly, writeOnly, or directional execution settings are applied.

The 41-case synthetic matrix is paired with parser-backed examples, enum and
default entry points, and discriminator mappings. Enum probes now validate
against `SchemaDefinition.getHighLevelSchema()` and use that schema's document
location and pointer, matching the existing openapi4j entry point. A nested
composition fixture exposes this behavior and is invalid under both validators
because its oneOf accepts two alternatives. Invalid and valid parser-backed
defaults and enums are included. The tests also require every expected
parser-backed entry-point key to be visited.

## Results

Focused comparison command (PowerShell, repository-configured Java 17):

```powershell
$env:JAVA_HOME='C:\dev\java\sdk\17'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn -q -pl core -am -Dtest=ValidatorMigrationComparisonTest '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Result: **44 tests, 0 failures, 0 errors**. Of 41 matrix cases, **26 had equal
validity**, **13 were rejected by openapi4j and accepted by networknt**, and
**2 were accepted by openapi4j and rejected by networknt**. The additional
parser-backed discriminator comparisons are reported separately below.

Combined core/rules command:

```powershell
$env:JAVA_HOME='C:\dev\java\sdk\17'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
mvn -q -pl core,rules -am '-Dtest=SchemaValidatorTest,ExamplesShouldValidateAgainstSchemaTest,ValidatorMigrationComparisonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

Result: **73 tests, 0 failures, 0 errors**: 44 comparison tests, 9 existing
`SchemaValidatorTest` tests, and 20 `ExamplesShouldValidateAgainstSchemaTest`
tests.

Parser-backed coverage includes 8 examples, 12 enum values (including a value
rejected by the enclosing oneOf), 2 defaults (one invalid), 1 external default,
the existing schema-name/allOf fixture, and equivalent explicit schema-name
and reference mapping cases under oneOf. Both validators rejected the invalid
composition enum and invalid default; both accepted the valid enum values and
default. The existing `FirstEvent` allOf discriminator case remains valid in
both.

### Validity differences and recommendations

Recommendations below are standards-based next steps for a future migration
decision, not approval to change production behavior. OpenAPI 3.0 discriminator
wording and nullable interactions leave some application-specific choices;
those are called out rather than presented as settled conformance findings.
Each case links directly to its `probe()` declaration in
[`ValidatorMigrationComparisonTest.java`](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java).

| Case | openapi4j 1.0.7 | networknt 2.0.8 | Recommendation | Evaluation |
|---|---|---|---|---|
| [`oneOf` overlapping alternatives with a discriminator mapping](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L48) | valid | invalid | **Accept networknt's rejection** if applying the ordinary `oneOf` rule: exactly one alternative must validate. OpenAPI describes the discriminator as a selection aid, not a replacement for the composition constraint. | OK, but discriminator schema should be validated (1) |
| [`anyOf` where the selected alternative fails but another passes](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L64) | invalid | valid | **Accept networknt's result** for ordinary `anyOf`: at least one alternative validates. If the API requires the mapped alternative specifically, model that contract explicitly rather than relying on the validator's discriminator shortcut. | see (1)|
| [Isolated missing discriminator value](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L101) (unrequired, untyped property; exactly one alternative validates) | invalid | valid | Defaults differ. OpenAPI says the discriminator property should be required, but absence here violates no schema requirement. **Add `required`** where the contract requires presence; otherwise accept the unconstrained-schema result. |OK if schema doesn't specify the discriminator property as required. This is checked by a rule. The `propertyName` under `discriminator` should present however when `discriminator` is present. To check if this is validated. |
| [Isolated null discriminator value](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L104) (unrequired, untyped property; exactly one alternative validates) | invalid | valid | The schema has no independent null restriction. **Add a string type (and required if appropriate)** when null is disallowed. Do not assume default networknt strictness rejects it. |OK, same as next|
| [Isolated non-string discriminator value](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L107) (untyped property; exactly one alternative validates) | valid | valid | Both accept it when no type constraint exists. **Add `type: string`** if the API contract requires textual discriminator values. The old constrained control rejects it because its property is typed. |OK, but maybe check if validated by a rule instead that discriminator property should be string|
| [Isolated unknown textual discriminator value](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L110) | invalid | invalid | Both reject this mapped oneOf probe. Keep an explicit unknown-value regression test if mapping behavior is relied on. |OK, should be covered by tests of (1)|
| [Inherited discriminator: `Child` allOf `$ref` to `Base`, mapped value](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L113) | valid | valid | Equal outcome on the actual inherited shape; this is the positive control, not proof of all inheritance cases. Retain a parser-backed fixture if this pattern is used. |
| [Inherited discriminator with unknown value](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L116) | invalid | valid | networknt does not reject the unknown value on this `Child`/`Base` allOf path when no schema constraint independently rejects it. **If unknown values must fail, add a value constraint or an explicit discriminator check**; do not count on implicit inherited-discriminator enforcement. ||
| [Inherited discriminator missing](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L119) | invalid | valid | No `required` constraint is present, so networknt accepts the instance. **Add `required`** when presence is part of the contract; otherwise record that networknt's default is permissive. ||
| [`nullable: true` with enum excluding null](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L139) | invalid | valid | OpenAPI says nullable adds null to the declared type and that other constraints may still reject it; enum interaction should be treated cautiously. **Keep null excluded with an explicit compatibility check if that is intended**, or include null in the enum when it is intended to pass. ||
| [Root `nullable: true` with type-less `oneOf`](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L142) or [`allOf`](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L150) | invalid | valid | OpenAPI 3.0 says nullable takes effect only with a `type` in the same Schema Object; networknt propagates through composition. **Do not rely on root nullable here**; put the nullable type on alternatives where null should be allowed, and retain a regression test. ||
| [Type-less schema applied to null](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L157) | invalid | valid | An omitted type imposes no JSON Schema type restriction. **Accept networknt's unconstrained result** unless the project deliberately requires the old implicit non-null default, in which case add an explicit constraint. ||
| `int32` outside the signed 32-bit range ([upper](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L166), [lower](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L169) probes) | invalid | valid | networknt's default OpenAPI dialect does not assert this format range. **Add explicit numeric bounds or a custom format assertion** if the API contract requires the range; otherwise accept the annotation-only result. |format should be validated|
| [`int64` above the signed 64-bit range](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L172) | invalid | valid | Same format-assertion gap. **Add explicit bounds or a custom assertion** if range enforcement is required. |format should be validated|
| [Invalid `byte`/base64 string](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L178) | invalid | valid | The default dialect does not validate this encoding. **Add a base64 constraint/custom assertion** if the API contract promises base64; otherwise accept the default behavior. |format should be validated|
| [Impossible `format: date` value `2023-02-29`](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L184) | valid | invalid | RFC 3339 full-date requires a real calendar date. **Accept networknt's rejection** as the standards-consistent result; the old validator is permissive here. |OK|

The isolated missing/null probes intentionally have no root `required` or
property `type` constraint, and their referenced oneOf alternatives are
non-overlapping and do not constrain `kind`. Thus their observed results measure
discriminator behavior rather than a separate property or branch failure. The
original required/type-constrained probes remain as controls.

### Mapping coverage and other equal outcomes

- The synthetic [`a -> A` schema-name-shorthand control](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L83)
  remains **invalid in both** engines. It also uses the existing implicit
  schema-name fallback and a branch enum, so it is **not evidence of general
  explicit schema-name mapping support**.
- Parser-backed overlapping oneOf cases compare equivalent mapping targets.
  With `child: SchemaNameChild`, both engines reject; openapi4j reports that it
  cannot select a schema for the value. With
  `child: "#/components/schemas/ReferenceChild"`, openapi4j accepts and
  networknt rejects. Since both referenced alternatives accept the instance,
  networknt's oneOf rejection is expected from the exactly-one rule. **Prefer
  non-overlapping oneOf branches and canonical reference mappings**; if overlap
  is intentional, use anyOf to express the at-least-one rule. The schema-name
  case does not establish support for short-name mappings in this oneOf path;
  see the [parser-backed comparison](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L428)
  and the [mapping fixture](../core/src/test/resources/io/github/belgif/rest/guide/validator/rules/migrationComparison.yaml#L25);
  do not rely on that form without a separate compatibility decision.
- A [nested unrelated discriminator inside an allOf sibling](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L122) passes in both
  implementations, matching the shape of the existing unrelated-discriminator
  regression fixture. The parent does not acquire the nested child's
  discriminator.
- Equal-validity controls include [valid byte/base64](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L175),
  [valid date](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L181),
  [relative uri-reference](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L193),
  [invalid date-time](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L187),
  [required property present as null](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L160),
  [exclusive minimum](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L196)
  and [maximum](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L199),
  and [additionalProperties with allOf](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L202).
  Directional [readOnly](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L215)
  and [writeOnly](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L218)
  behavior remains untested; the comparison intentionally leaves execution
  modes unset.
- A literal [`$ref` property](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L221)
  and an [escaped JSON Pointer component](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L224)
  validate in both. The existing external/relative/circular-reference fixture
  resolves using parser-supplied documents; its invalid Logos example is
  invalid in both ([parser-backed examples](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L380),
  [external default](../core/src/test/java/io/github/belgif/rest/guide/validator/core/ValidatorMigrationComparisonTest.java#L463)).
  No independent networknt fetching is enabled.

Some equal-validity failures have different diagnostics. For example, missing
required-property messages differ, as do URI, exclusive-minimum, and
additional-property messages. These are diagnostic differences, not validity
changes. Networknt's structured errors include keyword/instance-location
fields; exact order, prose, and detail counts are not migration acceptance
criteria.

## Recommendation and bounded remaining gaps

Keep the production validator and settings unchanged. The standards-based
recommendations above identify likely defaults for a later decision, but the
nullable/enum interaction, discriminator presence policy, and inherited
discriminator requirements still need confirmation against API contracts.
Where a difference is not wanted, first express it in the schema (for example,
`required`, `type`, or numeric bounds); use a narrow custom assertion only
where OpenAPI 3.0 cannot express the required behavior. Preserve rule identity,
severity, and actionable source/schema/instance locations, not exact diagnostic
text or ordering.

Remaining scope is limited and explicit: this is not an exhaustive audit of
all discriminator mapping forms (for example, relative/absolute references and
aliases), nested composition shapes, formats, reference-loading modes, or
production API documents. The parser-backed cases validate local supplied
documents only. No directional request/response mode, independent fetch, or
production-validator replacement was evaluated or approved.

## Sources

- [OpenAPI Specification 3.0.3 — Schema Object and discriminator](https://github.com/OAI/OpenAPI-Specification/blob/3.0.3/versions/3.0.3.md#schema-object)
- [networknt 2.0.8 OpenAPI dialect documentation](https://github.com/networknt/json-schema-validator/blob/2.0.8/doc/openapi.md)
- [networknt 2.0.8 `OpenApi30` dialect](https://github.com/networknt/json-schema-validator/blob/2.0.8/src/main/java/com/networknt/schema/dialect/OpenApi30.java)
- [networknt 2.0.8 discriminator validator](https://github.com/networknt/json-schema-validator/blob/2.0.8/src/main/java/com/networknt/schema/keyword/DiscriminatorValidator.java)
- [networknt 2.0.8 nullable type handling](https://github.com/networknt/json-schema-validator/blob/2.0.8/src/main/java/com/networknt/schema/utils/JsonNodeTypes.java)
- [networknt 2.0.8 format registry](https://github.com/networknt/json-schema-validator/blob/2.0.8/src/main/java/com/networknt/schema/format/Formats.java)
- Repository behavior under test: `core/src/main/java/org/openapi4j/schema/validator/v3/DiscriminatorValidator.java`,
  `core/src/main/java/org/openapi4j/schema/validator/v3/FormatValidator.java`,
  `core/src/main/java/io/github/belgif/rest/guide/validator/core/util/SchemaValidator.java`,
  and `core/src/main/java/io/github/belgif/rest/guide/validator/core/model/helper/BelgifOAI3Context.java`.
