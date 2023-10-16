package be.belgium.gcloud.rest.styleguide.validation.core.model;

import be.belgium.gcloud.rest.styleguide.validation.core.Line;
import be.belgium.gcloud.rest.styleguide.validation.core.OpenApiViolationAggregator;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;

public class OperationDefinition extends OpenApiDefinition<Operation> {

    @Getter
    @Setter
    private PathItem.HttpMethod method;

    public OperationDefinition(Operation model, OpenApiDefinition<?> parent, String identifier, String relativeJsonPointer, PathItem.HttpMethod method) {
        super(model, parent, identifier, relativeJsonPointer);
        this.method = method;
    }

    @Override
    public Operation getModel() {
        return super.getModel();
    }

    @Override
    public Line getLineNumber(OpenApiViolationAggregator aggregator) {
        Line lineNumber = this.getParent().getLineNumber(aggregator);
        return aggregator.getLineNumber(lineNumber, getModel().getOperationId());
    }
}
