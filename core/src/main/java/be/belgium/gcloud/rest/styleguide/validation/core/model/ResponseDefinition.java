package be.belgium.gcloud.rest.styleguide.validation.core.model;

import lombok.Getter;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;

import java.io.File;

public class ResponseDefinition extends OpenApiDefinition<APIResponse> {

    @Getter
    private final String statusCode;


    /**
     * Constructor for an inline definition
     *
     * @param openApiObject
     * @param parent
     * @param statusCode
     */
    public ResponseDefinition(APIResponse openApiObject, OperationDefinition parent, String statusCode) {
        super(openApiObject, parent, statusCode, "/responses/" + statusCode);
        this.statusCode = statusCode;
    }

    /**
     * Constructor for a definition under components
     *
     * @param openApiObject
     * @param name
     * @param openApiFile
     */
    public ResponseDefinition(APIResponse openApiObject, String name, File openApiFile) {
        super(openApiObject, name, openApiFile, "/components/responses/" + name);
        this.statusCode = null;
    }

    @Override
    public APIResponse getModel() {
        return super.getModel();
    }
}
