package io.github.belgif.rest.guide.validator.core.model.helper;

import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import lombok.Getter;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.*;

@Getter
public class PropertiesCollection {
    private final SchemaDefinition parent;
    private final Map<String, List<Schema>> properties = new HashMap<>();

    public PropertiesCollection(SchemaDefinition parent) {
        this.parent = parent;
    }

    public void addAll(Map<String, Schema> properties) {
        for (Map.Entry<String, Schema> entry : properties.entrySet()) {
            List<Schema> schemas;
            if (!this.properties.containsKey(entry.getKey())) {
                schemas = new ArrayList<>();
                this.properties.put(entry.getKey(), schemas);
            } else {
                schemas = this.properties.get(entry.getKey());
            }
            schemas.add(entry.getValue());
            this.properties.put(entry.getKey(), schemas);
        }
    }

    public void addPropertiesCollection(PropertiesCollection collection) {
        for (Map.Entry<String, List<Schema>> entry : collection.properties.entrySet()) {
            if (this.properties.containsKey(entry.getKey())) {
                List<Schema> schemas = this.properties.get(entry.getKey());
                schemas.addAll(entry.getValue());
                this.properties.put(entry.getKey(), schemas);
            } else {
                this.properties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public boolean containsProperty(String property) {
        return this.properties.containsKey(property);
    }

    public List<Schema> getPropertySchemas(String property) {
        return this.properties.get(property);
    }

    public Set<String> getPropertyNames() {
        return this.properties.keySet();
    }
}
