package be.belgium.gcloud.rest.styleguide.validation.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@ToString
public class ComponentProperties {
    ComponentType type;
    String propertyKey;
    String propertyName;
}

enum ComponentType{
    SCHEMA
}
