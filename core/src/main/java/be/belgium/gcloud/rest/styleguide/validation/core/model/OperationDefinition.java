package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.parser.JsonPointer;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

public class OperationDefinition extends OpenApiDefinition<Operation> {

    @Getter
    @Setter
    private PathItem.HttpMethod method;

    public OperationDefinition(Operation model, PathDefinition parent, PathItem.HttpMethod method) {
        super(model, parent, method.name()+" "+parent.getIdentifier(), JsonPointer.relative(method.toString().toLowerCase()));
        this.method = method;
    }

    @Override
    public Operation getModel() {
        return super.getModel();
    }

}
