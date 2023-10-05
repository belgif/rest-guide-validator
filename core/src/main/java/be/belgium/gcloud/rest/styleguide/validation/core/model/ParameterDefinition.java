package be.belgium.gcloud.rest.styleguide.validation.core.model;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import java.io.File;

public class ParameterDefinition<T extends Constructible> extends OpenApiDefinition<Parameter> {
    /**
     * Constructor for an inline definition
     *
     * @param openApiObject
     * @param parent
     */
    protected ParameterDefinition(Parameter openApiObject, OpenApiDefinition<?> parent, String name, String relativeJsonPath) {
        super(openApiObject, parent, name, relativeJsonPath);
    }

    /**
     * Constructor for a definition under components
     *
     * @param openApiObject
     * @param name
     * @param openApiFile
     */
    protected ParameterDefinition(Parameter openApiObject, String name, File openApiFile, String jsonPath) {
        super(openApiObject, name, openApiFile, jsonPath);
    }
}
