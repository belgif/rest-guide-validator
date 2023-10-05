package be.belgium.gcloud.rest.styleguide.validation.core.model;

import org.eclipse.microprofile.openapi.models.media.Schema;

import java.io.File;

public class SchemaDefinition extends OpenApiDefinition<Schema> {

    /**
     * Constructor for an inline definition
     *
     * @param openApiObject
     * @param parent
     */
    public SchemaDefinition(Schema openApiObject, OpenApiDefinition<?> parent, String name, String relativeJsonPath) {
        super(openApiObject, parent, name, relativeJsonPath);
    }

    /**
     * Constructor for a definition under components
     *
     * @param openApiObject
     * @param name
     * @param openApiFile
     */
    public SchemaDefinition(Schema openApiObject, String name, File openApiFile) {
        super(openApiObject, name, openApiFile, "/components/schemas/" + name);
    }
}
