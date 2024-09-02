package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.io.File;

public class SchemaDefinition extends OpenApiDefinition<Schema> {

    /**
     * Constructor for an inline definition
     *
     * @param openApiObject
     * @param parent
     */
    public SchemaDefinition(Schema openApiObject, OpenApiDefinition<?> parent, JsonPointer relativeJsonPath) {
        super(openApiObject, parent, null, relativeJsonPath);
    }

    public SchemaDefinition(Schema openApiObject, OpenApiDefinition<?> parent) {
        super(openApiObject, parent, null, JsonPointer.relative("schema"));
    }

    /**
     * Constructor for a definition under components
     *
     * @param openApiObject
     * @param name
     * @param openApiFile
     */
    public SchemaDefinition(Schema openApiObject, String name, File openApiFile, Parser.ParserResult result) {
        super(openApiObject, name, openApiFile, new JsonPointer("/components/schemas").add(name), result);
    }

    /**
     * <p>
     * A SchemaDefinition is considered high-level if (at least) one of the the following conditions apply:
     * <ol>
     * <li>It is a top-level schema (defined in components/schemas)</li>
     * <li>It has a parent which is not of type schema (e.g. Request, Response)</li>
     * <li>It is NOT part of any/one/all Of</li>
     * </ol>
     * </p>
     */
    public boolean isHighLevelSchema() {
        return this.definitionType.equals(DefinitionType.TOP_LEVEL) ||
                this.getParent() == null ||
                !(this.getParent() instanceof SchemaDefinition) ||
                !this.getJsonPointer().toString().matches("^.*/(all|any|one)Of/\\d*?$");
    }

    /**
     * @return The closes parent that is considered a highLevelSchema
     * @see #isHighLevelSchema()
     */
    public SchemaDefinition getHighLevelSchema() {
        if (isHighLevelSchema()) {
            return this;
        } else {
            return ((SchemaDefinition) this.getParent()).getHighLevelSchema();
        }
    }

    @Override
    public Schema getModel() {
        return super.getModel();
    }
}
