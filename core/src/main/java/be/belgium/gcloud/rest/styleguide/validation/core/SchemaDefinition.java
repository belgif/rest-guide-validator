package be.belgium.gcloud.rest.styleguide.validation.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.microprofile.openapi.models.media.Schema;

@Getter
@Builder
@AllArgsConstructor
@ToString
public class SchemaDefinition {
    OpenApiDefinitionLocation parentDefinitionLocation;
    String parentName;
    Schema schema;
}
