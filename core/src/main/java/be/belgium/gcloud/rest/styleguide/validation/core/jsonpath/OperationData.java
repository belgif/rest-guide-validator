package be.belgium.gcloud.rest.styleguide.validation.core.jsonpath;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.microprofile.openapi.models.PathItem;

@Getter
@Builder
@ToString
public class OperationData {
    public PathItem.HttpMethod operation;
    public String operationId;
    public String[] produces;
    public String[] consumes;
}
