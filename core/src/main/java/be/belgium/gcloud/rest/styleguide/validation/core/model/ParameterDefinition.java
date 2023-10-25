package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;

public class ParameterDefinition extends OpenApiDefinition<Parameter> {
    /**
     * Constructor for an inline definition
     *
     */
    public ParameterDefinition(Parameter openApiObject, OpenApiDefinition<?> parent, String name, int index) {
        super(openApiObject, parent, name, JsonPointer.relative("parameters").add(index));
    }

    @Override
    public Parameter getModel() {
        return super.getModel();
    }
}
