package io.github.belgif.rest.guide.validator.core.model;

import io.github.belgif.rest.guide.validator.core.parser.JsonPointer;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

public class OperationDefinition extends OpenApiDefinition<Operation> {

    @Getter
    @Setter
    private PathItem.HttpMethod method;

    public OperationDefinition(Operation model, PathItemDefinition parent, PathItem.HttpMethod method) {
        super(model, parent, method.name()+" "+parent.getIdentifier(), JsonPointer.relative(method.toString().toLowerCase()));
        this.method = method;
    }

    @Override
    public Operation getModel() {
        return super.getModel();
    }

}
