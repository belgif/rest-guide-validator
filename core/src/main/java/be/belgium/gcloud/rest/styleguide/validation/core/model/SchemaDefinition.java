package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.io.File;

public class SchemaDefinition extends OpenApiDefinition<Schema> {

    /**
     * Constructor for an inline definition
     *
     * @param openApiObject
     * @param parent
     */
    public SchemaDefinition(Schema openApiObject, OpenApiDefinition<?> parent, String name, JsonPointer relativeJsonPath) {
        super(openApiObject, parent, name, relativeJsonPath.add(name));
    }

    public SchemaDefinition(Schema openApiObject, OpenApiDefinition<?> parent, String name) {
        super(openApiObject, parent, name, JsonPointer.relative("schema").add(name));
    }

    /**
     * Constructor for a definition under components
     *
     * @param openApiObject
     * @param name
     * @param openApiFile
     */
    public SchemaDefinition(Schema openApiObject, String name, File openApiFile) {
        super(openApiObject, name, openApiFile, new JsonPointer("/components/schemas").add(name));
    }

    @Override
    public Schema getModel() {
        return super.getModel();
    }
}
