package org.openapi4j.schema.validator.v3;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.openapi4j.core.model.reference.Reference;
import org.openapi4j.core.model.reference.ReferenceRegistry;
import org.openapi4j.core.model.v3.OAI3;
import org.openapi4j.core.model.v3.OAI3SchemaKeywords;
import org.openapi4j.core.validation.ValidationResult;
import org.openapi4j.core.validation.ValidationResults;
import org.openapi4j.schema.validator.BaseJsonValidator;
import org.openapi4j.schema.validator.ValidationContext;
import org.openapi4j.schema.validator.ValidationData;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;

import static org.openapi4j.core.model.reference.Reference.ABS_REF_FIELD;
import static org.openapi4j.core.model.v3.OAI3SchemaKeywords.*;
import static org.openapi4j.core.validation.ValidationSeverity.ERROR;

/**
 * This class overrides <a href="https://github.com/openapi4j/openapi4j/blob/master/openapi-schema-validator/src/main/java/org/openapi4j/schema/validator/v3/DiscriminatorValidator.java">this class of the OpenApi4j SchemaValidator</a>.
 * Validation against some schemas using allOf and discriminator didn't work.
 * This was caused by getReferences() (not existing in this file anymore).
 * All references in all subschemas and properties within an allOf schema were checked for a discriminator.
 * Now getReference() is used and only the immediate schema items within an allOf are checked for a discriminator instead of all nested nodes.
 * <p>
 * Also added method resolveRelativeRef()
 */

abstract class DiscriminatorValidator extends BaseJsonValidator<OAI3> {
    private static final ValidationResult INVALID_SCHEMA_ERR = new ValidationResult(ERROR, 1003, "Schema selection can't be made for discriminator '%s' with value '%s'.");
    private static final ValidationResult INVALID_PROPERTY_ERR = new ValidationResult(ERROR, 1004, "Property name in schema is not set.");
    private static final ValidationResult INVALID_PROPERTY_CONTENT_ERR = new ValidationResult(ERROR, 1005, "Property name in content '%s' is not set.");

    private static final ValidationResults.CrumbInfo CRUMB_INFO = new ValidationResults.CrumbInfo(DISCRIMINATOR, true);

    private static final String SCHEMAS_PATH = "#/components/schemas/";

    final List<SchemaValidator> validators = new ArrayList<>();
    private final String arrayType;
    private JsonNode discriminatorNode;
    private String discriminatorPropertyName;
    private JsonNode discriminatorMapping;
    private final ValidationResults.CrumbInfo crumbInfo;

    DiscriminatorValidator(final ValidationContext<OAI3> context,
                           final JsonNode schemaNode,
                           final JsonNode schemaParentNode,
                           final SchemaValidator parentSchema,
                           final String arrayType) {

        super(context, schemaNode, schemaParentNode, parentSchema);

        this.arrayType = arrayType;
        crumbInfo = new ValidationResults.CrumbInfo(arrayType, true);

        // Setup discriminator behaviour for anyOf, oneOf or allOf
        setupDiscriminator(context, schemaNode, schemaParentNode, parentSchema);
    }

    @Override
    public boolean validate(final JsonNode valueNode, final ValidationData<?> validation) {
        if (discriminatorNode != null) {
            String discriminatorValue = getDiscriminatorValue(valueNode, validation);
            if (discriminatorValue != null) {
                if (ALLOF.equals(arrayType)) {
                    validateAllOf(valueNode, discriminatorValue, validation);
                } else {
                    validateOneAnyOf(valueNode, discriminatorValue, validation);
                }
            }
        } else {
            validateWithoutDiscriminator(valueNode, validation);
        }

        return false;
    }

    private void validateAllOf(final JsonNode valueNode, final String discriminatorValue, final ValidationData<?> validation) {
        if (!checkAllOfValidator(discriminatorValue)) {
            validation.add(CRUMB_INFO, INVALID_SCHEMA_ERR, discriminatorPropertyName, discriminatorValue);
            return;
        }

        validate(() -> {
            for (SchemaValidator validator : validators) {
                validator.validateWithContext(valueNode, validation);
            }
        });
    }

    private void validateOneAnyOf(final JsonNode valueNode, final String discriminatorValue, final ValidationData<?> validation) {
        SchemaValidator validator = getOneAnyOfValidator(discriminatorValue);
        if (validator == null) {
            validation.add(CRUMB_INFO, INVALID_SCHEMA_ERR, discriminatorPropertyName, discriminatorValue);
            return;
        }

        validate(() -> validator.validateWithContext(valueNode, validation));
    }

    /**
     * Validate array keyword with default behaviour.
     *
     * @param valueNode  The current value node.
     * @param validation The validation results.
     */
    abstract void validateWithoutDiscriminator(final JsonNode valueNode, final ValidationData<?> validation);

    private void setupDiscriminator(final ValidationContext<OAI3> context,
                                    final JsonNode schemaNode,
                                    final JsonNode schemaParentNode,
                                    final SchemaValidator parentSchema) {

        if (ALLOF.equals(arrayType)) {
            setupAllOfDiscriminatorSchemas(context, schemaNode, schemaParentNode, parentSchema);
        } else {
            setupAnyOneOfDiscriminatorSchemas(context, schemaNode, schemaParentNode, parentSchema);
        }

        if (discriminatorNode != null) {
            JsonNode propertyNameNode = discriminatorNode.get(PROPERTYNAME);
            if (propertyNameNode == null) {
                // Property name in schema is not set. This will result in error on validate call.
                return;
            }

            discriminatorPropertyName = propertyNameNode.textValue();
            discriminatorMapping = discriminatorNode.get(MAPPING);
        }
    }

    private void setupAllOfDiscriminatorSchemas(final ValidationContext<OAI3> context,
                                                final JsonNode schemaNode,
                                                final JsonNode schemaParentNode,
                                                final SchemaValidator parentSchema) {

        JsonNode allOfNode = getParentSchemaNode().get(ALLOF);
        ReferenceRegistry refRegistry = context.getContext().getReferenceRegistry();

        for (JsonNode allOfNodeItem : allOfNode) {
            JsonNode refNode = getReference(allOfNodeItem);
            if (refNode != null) {
                Reference reference = refRegistry.getRef(refNode.textValue());
                discriminatorNode = reference.getContent().get(DISCRIMINATOR);
                if (discriminatorNode != null) {
                    setupAllOfDiscriminatorSchemas(schemaNode, refNode, reference, schemaParentNode, parentSchema);
                    return;
                }
            }
        }

        // Add default schemas
        for (JsonNode node : schemaNode) {
            validators.add(new SchemaValidator(context, crumbInfo, node, schemaParentNode, parentSchema));
        }
    }

    private void setupAnyOneOfDiscriminatorSchemas(final ValidationContext<OAI3> context,
                                                   final JsonNode schemaNode,
                                                   final JsonNode schemaParentNode,
                                                   final SchemaValidator parentSchema) {

        discriminatorNode = getParentSchemaNode().get(DISCRIMINATOR);

        for (JsonNode node : schemaNode) {
            validators.add(new SchemaValidator(context, crumbInfo, node, schemaParentNode, parentSchema));
        }
    }

    private void setupAllOfDiscriminatorSchemas(final JsonNode schemaNode,
                                                final JsonNode refNode,
                                                final Reference reference,
                                                final JsonNode schemaParentNode,
                                                final SchemaValidator parentSchema) {

        for (JsonNode node : schemaNode) {
            JsonNode refValueNode = getReference(node);

            if (refNode.equals(refValueNode)) { // Add the parent schema
                ValidationResults.CrumbInfo refCrumbInfo = new ValidationResults.CrumbInfo(reference.getRef(), true);
                validators.add(new SchemaValidator(context, refCrumbInfo, reference.getContent(), schemaParentNode, parentSchema));
            } else { // Add the other items
                validators.add(new SchemaValidator(context, crumbInfo, node, schemaParentNode, parentSchema));
            }
        }
    }

    private JsonNode getReference(JsonNode node) {
        // Prefer absolute reference value
        JsonNode refNode = node.get(ABS_REF_FIELD);
        if (refNode == null) {
            refNode = node.get(OAI3SchemaKeywords.$REF);
        }

        return refNode;
    }

    private String getDiscriminatorValue(final JsonNode valueNode, final ValidationData<?> validation) {
        // check discriminator definition
        if (discriminatorPropertyName == null) {
            validation.add(CRUMB_INFO, INVALID_PROPERTY_ERR);
            return null;
        }
        // check discriminator in content
        JsonNode discriminatorPropertyNameNode = valueNode.get(discriminatorPropertyName);
        if (discriminatorPropertyNameNode == null) {
            validation.add(CRUMB_INFO, INVALID_PROPERTY_CONTENT_ERR, discriminatorPropertyName);
            return null;
        }

        return discriminatorPropertyNameNode.textValue();
    }

    private boolean checkAllOfValidator(final String discriminatorValue) {
        String ref = null;

        // Explicit case with mapping
        if (discriminatorMapping != null) {
            JsonNode mappingNode = discriminatorMapping.get(discriminatorValue);
            if (mappingNode != null) {
                ref = mappingNode.textValue();
            }
        }

        // Implicit case, the value must match exactly one schema in "#/components/schemas/"
        if (ref == null) {
            ref = SCHEMAS_PATH + discriminatorValue;
        }

        // Check if Schema Object exists
        // Modification for Belgif validator: Resolve relative refs
        String resolvedRef = resolveRelativeRef(ref);
        return context.getContext().getReferenceRegistry().getRef(resolvedRef) != null || refExists(resolvedRef);
    }

    private boolean refExists(String ref) {
        try {
            URL baseUrl = URI.create(ref.split("#/")[0]).toURL();
            String internalRef = ref.split("#/")[1];
            File file = new File(baseUrl.getFile());
            JsonFactory factory;
            if (file.exists()) {
                if (file.getName().endsWith(".yaml") || file.getName().endsWith(".yml")) {
                    factory = new YAMLFactory();
                } else {
                    factory = new JsonFactory();
                }
                try {
                    ObjectMapper mapper = new ObjectMapper(factory);
                    JsonNode referencedFile = mapper.readTree(file);
                    JsonPointer jsonPointer = JsonPointer.compile("/"+internalRef);
                    JsonNode resolvedNode = referencedFile.at(jsonPointer);
                    return resolvedNode != null && !(resolvedNode instanceof MissingNode);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * Custom method for Belgif Validator
     * Resolves the reference within the discriminatorMapping
     */
    private String resolveRelativeRef(String ref) {
        // create list of files traversed through references to the discriminator
        List<String> fileCrumbs = new ArrayList<>();
        addFirstFileCrumb(fileCrumbs, ref);
        SchemaValidator schemaValidator = this.getParentSchema();
        while (schemaValidator != null) {
            addExternalFileCrumb(schemaValidator.getCrumbInfo(), fileCrumbs);
            schemaValidator = schemaValidator.getParentSchema();
        }
        Collections.reverse(fileCrumbs); // So refs are in order starting from the 'entry' file.

        try {
            Path initialOpenApiPath = Paths.get(context.getContext().getBaseUrl().toURI()); // absolute file name of the initial OpenAPI file
            return buildRef(fileCrumbs, getComponentRef(ref), initialOpenApiPath);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void addFirstFileCrumb(List<String> fileCrumbs, String ref) {
        if (!ref.startsWith("#/")) { // There is a file in the ref of the Discriminator Mapping
            fileCrumbs.add(ref.split("#/")[0]);
        }
        if (this.getSchemaNode().has(DISCRIMINATOR)) { //The discriminator is directly in the AllOf schema
            addExternalFileCrumb(this.crumbInfo, fileCrumbs);
        } else {
            for (SchemaValidator childValidator : validators) { // The discriminator is in one of the referenced schemas
                if (childValidator.getSchemaNode().has(DISCRIMINATOR)) {
                    addExternalFileCrumb(childValidator.getCrumbInfo(), fileCrumbs);
                    break;
                }
            }
        }
    }

    /**
     * Returns only the reference without file name
     */
    private static String getComponentRef(String ref) {
        String[] refSplit = ref.split("#/");
        return "#/" + refSplit[refSplit.length - 1];
    }

    /**
     * Returns a reference with absolute file path to the given ref
     * based on the files traversed to the discriminator
     */
    private static String buildRef(List<String> fileCrumbs, String ref, Path initialOpenApiPath) {
        if (fileCrumbs.isEmpty()) {
            return ref;
        }

        // build path to the ref, combining all paths traversed to it
        StringBuilder sb = new StringBuilder();
        for (String crumb : fileCrumbs) {
            String fileName = getFileNameFromPath(crumb);
            String pathInCrumb = crumb.replace(fileName, ""); //Ditch filename so only folder hopping in refs is taken into account.
            if (!pathInCrumb.isEmpty()) {
                sb.append(pathInCrumb);
            }
        }
        sb.append(getFileNameFromPath(fileCrumbs.get(fileCrumbs.size() - 1))); // append file containing the ref, i.e. with the discriminator
        return initialOpenApiPath.getParent().resolve(sb.toString()).normalize().toFile().toURI() + ref;
    }

    private static String getFileNameFromPath(String filePath) {
        String[] splits = filePath.split("/");
        return splits[splits.length - 1];
    }

    private static void addExternalFileCrumb(ValidationResults.CrumbInfo crumbInfo, List<String> refCrumbs) {
        if (crumbInfo != null && crumbInfo.crumb() != null && crumbInfo.crumb().contains("#/") && !crumbInfo.crumb().startsWith("#/")) { // Crumb contains an external ref
            refCrumbs.add(crumbInfo.crumb().split("#/")[0]); // Add file reference to refCrumbs
        }
    }

    // End of customizations for Belgif Validator

    private SchemaValidator getOneAnyOfValidator(final String discriminatorValue) {
        // Explicit case with mapping
        if (discriminatorMapping != null) {
            JsonNode mappingNode = discriminatorMapping.get(discriminatorValue);
            if (mappingNode != null) {
                String ref = mappingNode.textValue();
                SchemaValidator validator = getOneAnyOfValidator(ref, String::equals);
                if (validator != null) {
                    return validator;
                }
            }
        }

        // Implicit case, the value must match exactly one of the schemas name regardless path
        return getOneAnyOfValidator(discriminatorValue, String::endsWith);
    }

    private SchemaValidator getOneAnyOfValidator(final String value,
                                                 final BiPredicate<String, String> checker) {

        for (SchemaValidator validator : validators) {
            JsonNode refNode = validator.getSchemaNode().get($REF);
            if (refNode != null && checker.test(refNode.textValue(), value)) {
                return validator;
            }
        }

        return null;
    }
}

