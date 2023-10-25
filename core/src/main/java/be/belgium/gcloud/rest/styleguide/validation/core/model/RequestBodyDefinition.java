package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;

import java.io.File;

public class RequestBodyDefinition extends OpenApiDefinition<RequestBody> {

    /**
     * Constructor for an inline definition in an operation
     *
     * @param openApiObject
     * @param parent
     */
    public RequestBodyDefinition(RequestBody openApiObject, OperationDefinition parent) {
        super(openApiObject, parent, "requestBody", JsonPointer.relative("requestBody"));
    }

    /**
     * Constructor for a definition under components
     *
     */
    public RequestBodyDefinition(RequestBody openApiObject, String name, File openApiFile) {
        super(openApiObject, name, openApiFile, new JsonPointer("/components/requestBodies/").add(name));
    }

    @Override
    public RequestBody getModel() {
        return super.getModel();
    }
}
