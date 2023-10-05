package be.belgium.gcloud.rest.styleguide.validation.core.model;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

import java.io.File;

public class OperationDefinition extends OpenApiDefinition<Operation> {

    @Getter
    @Setter
    private PathItem.HttpMethod method;

    @Getter
    @Setter
    private String path;

    public OperationDefinition(Operation openApiObject, String path, PathItem.HttpMethod method, File openApiFile) {
        super(openApiObject, method.toString() + " " + path, openApiFile, "/paths/" + path + "/" + method.toString().toLowerCase());
        this.method = method;
        this.path = path;
    }

    @Override
    public Operation getModel() {
        return super.getModel();
    }
}
