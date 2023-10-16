package be.belgium.gcloud.rest.styleguide.validation.core.model;

import org.eclipse.microprofile.openapi.models.parameters.Parameter;

import java.io.File;

public class ParameterDefinition extends OpenApiDefinition<Parameter> {
    /**
     * Constructor for an inline definition
     *
     * @param openApiObject
     * @param parent
     */
    public ParameterDefinition(Parameter openApiObject, OpenApiDefinition<?> parent, String name, String relativeJsonPath) {
        super(openApiObject, parent, name, relativeJsonPath);
    }

    /**
     * Constructor for a definition under components
     *
     * @param openApiObject
     * @param name
     * @param openApiFile
     */
    public ParameterDefinition(Parameter openApiObject, String name, File openApiFile, String jsonPath) {
        super(openApiObject, name, openApiFile, jsonPath);
    }

    @Override
    public Parameter getModel() {
        return super.getModel();
    }
}
