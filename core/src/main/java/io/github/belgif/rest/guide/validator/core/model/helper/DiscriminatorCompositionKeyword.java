package io.github.belgif.rest.guide.validator.core.model.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaContext;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.keyword.BaseKeywordValidator;
import com.networknt.schema.keyword.Keyword;
import com.networknt.schema.keyword.KeywordType;
import com.networknt.schema.keyword.KeywordValidator;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.utils.JsonNodeTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Keeps the existing discriminator selection semantics; networknt treats discriminator as an annotation.
 */
final class DiscriminatorCompositionKeyword implements Keyword {
    private final KeywordType keyword;
    private final Map<String, JsonNode> documents;

    DiscriminatorCompositionKeyword(KeywordType keyword, Map<String, JsonNode> documents) {
        this.keyword = keyword;
        this.documents = documents;
    }

    @Override
    public String getValue() {
        return keyword.getValue();
    }

    @Override
    public KeywordValidator newValidator(SchemaLocation location, JsonNode node, Schema parent,
                                         SchemaContext context) throws Exception {
        JsonNode discriminator = null;
        if (keyword == KeywordType.ALL_OF) {
            // Only immediate referenced parents contribute an allOf discriminator, never nested properties.
            for (JsonNode branch : node) {
                if (branch.has("$ref")) {
                    discriminator = resolve(branch.get("$ref").asText()).get("discriminator");
                    if (discriminator != null) {
                        break;
                    }
                }
            }
        } else {
            discriminator = parent.getSchemaNode().get("discriminator");
        }
        if (discriminator == null) {
            return keyword.newValidator(location, node, parent, context);
        }
        return new Validator(location, node, parent, context, discriminator,
                keyword.newValidator(location, node, parent, context));
    }

    private JsonNode resolve(String ref) {
        SchemaLocation location = SchemaLocation.of(ref);
        JsonNode document = documents.get(location.getAbsoluteIri().toString());
        if (document == null) {
            throw new IllegalStateException("No parsed document for schema reference " + ref);
        }
        return document.at(location.getFragment().toString());
    }

    private final class Validator extends BaseKeywordValidator {
        private final JsonNode discriminator;
        private final KeywordValidator standardValidator;
        private final List<Schema> branches = new ArrayList<>();

        Validator(SchemaLocation location, JsonNode node, Schema parent, SchemaContext context,
                  JsonNode discriminator, KeywordValidator standardValidator) {
            super(DiscriminatorCompositionKeyword.this, node, location, parent, context);
            this.discriminator = discriminator;
            this.standardValidator = standardValidator;
            for (int i = 0; i < node.size(); i++) {
                branches.add(context.newSchema(location.append(i), node.get(i), parent));
            }
        }

        @Override
        public void validate(ExecutionContext execution, JsonNode node, JsonNode root, NodePath instanceLocation) {
            if (node.isNull() && JsonNodeTypes.isNullableAncestor(execution)) {
                standardValidator.validate(execution, node, root, instanceLocation);
                return;
            }
            String property = discriminator.get("propertyName").asText();
            JsonNode value = node.get(property);
            if (value == null) {
                addError(execution, node, instanceLocation, "Property name in content '" + property + "' is not set.");
                return;
            }
            String name = value.textValue();
            if (name == null) {
                return;
            }
            JsonNode mapping = discriminator.path("mapping").get(name);
            String mappedRef = mapping == null ? null : mapping.asText();
            if (keyword == KeywordType.ALL_OF) {
                String ref = mappedRef == null
                        ? schemaLocation.resolve("#/components/schemas/" + name).toString() : mappedRef;
                if (resolve(ref).isMissingNode()) {
                    selectionError(execution, node, instanceLocation, property, name);
                    return;
                }
                for (int i = 0; i < branches.size(); i++) {
                    validateBranch(i, execution, node, root, instanceLocation);
                }
            } else {
                int selected = -1;
                if (mappedRef != null) {
                    selected = findBranch(mappedRef, false);
                }
                if (selected == -1) {
                    selected = findBranch(name, true);
                }
                if (selected == -1) {
                    selectionError(execution, node, instanceLocation, property, name);
                } else {
                    validateBranch(selected, execution, node, root, instanceLocation);
                }
            }
        }

        private int findBranch(String value, boolean implicit) {
            for (int i = 0; i < branches.size(); i++) {
                JsonNode ref = branches.get(i).getSchemaNode().get("$ref");
                if (ref != null && (implicit ? ref.asText().endsWith(value) : ref.asText().equals(value))) {
                    return i;
                }
            }
            return -1;
        }

        private void validateBranch(int index, ExecutionContext execution, JsonNode node, JsonNode root,
                                    NodePath instanceLocation) {
            execution.evaluationPathAddLast(index);
            try {
                branches.get(index).validate(execution, node, root, instanceLocation);
            } finally {
                execution.evaluationPathRemoveLast();
            }
        }

        private void selectionError(ExecutionContext execution, JsonNode node, NodePath location,
                                    String property, String value) {
            addError(execution, node, location,
                    "Schema selection can't be made for discriminator '" + property + "' with value '" + value + "'.");
        }

        private void addError(ExecutionContext execution, JsonNode node, NodePath location, String message) {
            execution.addError(Error.builder().keyword("discriminator").schemaLocation(schemaLocation)
                    .evaluationPath(execution.getEvaluationPath()).instanceLocation(location)
                    .instanceNode(node).schemaNode(discriminator).message(message).build());
        }
    }
}
