package be.belgium.gcloud.rest.styleguide.validation.core.model;

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
        super(openApiObject, parent, "requestBody", "/requestBody");
    }

    /**
     * Constructor for a definition under components
     *
     */
    public RequestBodyDefinition(RequestBody openApiObject, String name, File openApiFile) {
        super(openApiObject, name, openApiFile, "/components/requestBodies/" + name);
    }
}
