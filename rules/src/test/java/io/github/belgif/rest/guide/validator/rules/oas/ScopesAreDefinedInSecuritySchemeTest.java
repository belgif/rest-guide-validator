package io.github.belgif.rest.guide.validator.rules.oas;

import io.github.belgif.rest.guide.validator.rules.AbstractOasRuleTest;
import org.junit.jupiter.api.Test;

class ScopesAreDefinedInSecuritySchemeTest extends AbstractOasRuleTest {

    @Test
    void testValidOpenApi() {
        assertNoViolations(callRules("openapi.yaml"));
    }

    @Test
    void testInvalidOpenApi() {
        assertErrorCount(1, callRules("invalidOpenapi.yaml"));
    }

    @Test
    void testInvalidGlobalOpenApi() {
        assertErrorCount(1, callRules("invalidGlobalOpenapi.yaml"));
    }

    @Test
    void testValidOpenApiWithRedundantSecuritySchemesInExternalFiles() {
        assertNoViolations(callRules("externalNamingConflicts.yaml"));
    }

    @Test
    void testSecuritySchemeWithImplicitReferenceToEntryFile() {
        assertNoViolations(callRules("references/inEntryFile.yaml"));
    }

    @Test
    void securitySchemeWithRefIsValidated() {
        assertErrorCount(1, callRules("securitySchemeWithRef/openapi.yaml"));
    }}
