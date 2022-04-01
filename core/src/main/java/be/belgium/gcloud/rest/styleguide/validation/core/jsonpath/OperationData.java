package be.belgium.gcloud.rest.styleguide.validation.core.jsonpath;

import be.belgium.gcloud.rest.styleguide.validation.core.OperationEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class OperationData {
    public OperationEnum operation;
    public String operationId;
    public String[] produces;
    public String[] consumes;
}
