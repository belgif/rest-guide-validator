package io.github.belgif.rest.guide.validator.core.util;

import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CircularReferenceUtil {

    private CircularReferenceUtil() {
    }

    public static void validateCircularReferences(Parser.ParserResult result) {
        for (OpenApiDefinition<?> def : result.getAllDefinitions()) {
            OpenApiDefinition<?> circularDefinition = findCircularReference(def, new ArrayList<>(), result);
            if (circularDefinition != null) {
                log.error("{} contains a circular reference to itself", circularDefinition.getPrintableJsonPointer());
                result.setParsingValid(false);
                return;
            }
            if(def instanceof SchemaDefinition) {
                SchemaDefinition circularDiscriminatorDefinition = findCircularDiscriminator((SchemaDefinition) def, List.of(), result);
                if (circularDiscriminatorDefinition != null) {
                    log.error("{} contains a circular reference to itself via discriminators", circularDiscriminatorDefinition.getPrintableJsonPointer());
                    result.setParsingValid(false);
                    return;
                }
            }
        }
    }

    private static SchemaDefinition findCircularDiscriminator(SchemaDefinition def, List<SchemaDefinition> visited, Parser.ParserResult result) {
        if(visited.contains(def)) {
            return def;
        }
        List<SchemaDefinition> refs = new ArrayList<>();
        addDiscriminatorRefs(refs, def, result);
        for (SchemaDefinition ref : refs) {
            List<SchemaDefinition> nextVisit = new ArrayList<>(visited);
            nextVisit.add(def);
            SchemaDefinition circularDefinition = findCircularDiscriminator(ref, nextVisit, result);
            if(circularDefinition != null) {
                return circularDefinition;
            }
        }
        return null;
    }


    /**
     *
     * @param def - a definition referenced by the last definition in visited, e.g. D if C -> D
     * @param visited - chain of definitions referencing one another e.g.  A -> B -> C
     * @param result
     * @return  Whether there's an unsafe circular reference concerning 'def' or one of the definitions it references
     */
    private static OpenApiDefinition<?> findCircularReference(OpenApiDefinition<?> def, List<OpenApiDefinition<?>> visited, Parser.ParserResult result) {
        if (visited.contains(def)) {
            return def;
        }
        for (OpenApiDefinition<?> ref : getUsedSchemas(def, result)) {
            List<OpenApiDefinition<?>> nextVisit = new ArrayList<>(visited);
            nextVisit.add(def);

            OpenApiDefinition<?> circularDefinition = findCircularReference(ref, nextVisit, result);
            if (circularDefinition != null) {
                return circularDefinition;
            }
        }
        return null;
    }

    private static List<OpenApiDefinition<?>> getUsedSchemas(OpenApiDefinition<?> definition, Parser.ParserResult result) {
        List<OpenApiDefinition<?>> usedSchemas = new ArrayList<>();

        if (definition.hasReference()) {
            usedSchemas.add(result.resolve(definition.getModel()));
        }

        if (definition instanceof SchemaDefinition schemaDefinition) {
            addSchemaCollectionRefs(usedSchemas, schemaDefinition.getModel().getAllOf(), result);
            addSchemaCollectionRefs(usedSchemas, schemaDefinition.getModel().getOneOf(), result);
            addSchemaCollectionRefs(usedSchemas, schemaDefinition.getModel().getAnyOf(), result);

            if (schemaDefinition.getModel().getNot() != null) {
                usedSchemas.add(result.resolve(schemaDefinition.getModel().getNot()));
            }
        }

        return usedSchemas;
    }

    private static void addSchemaCollectionRefs(List<OpenApiDefinition<?>> usedSchemas, List<Schema> schemaDefinitionList, Parser.ParserResult result) {
        if (schemaDefinitionList == null || schemaDefinitionList.isEmpty()) {
            return;
        }
        usedSchemas.addAll(schemaDefinitionList.stream().map(result::resolve).toList());
    }

    private static void addDiscriminatorRefs(List<SchemaDefinition> refs, SchemaDefinition schemaDefinition, Parser.ParserResult result) {
        if (schemaDefinition.getModel().getDiscriminator() == null || schemaDefinition.getModel().getDiscriminator().getMapping() == null) {
            return;
        }
        refs.addAll(schemaDefinition.getModel().getDiscriminator().getMapping().values().stream()
                .map(m -> result.resolveDiscriminatorMapping(schemaDefinition, m).orElseThrow(() -> new IllegalStateException("Reference does not exist but somehow was not catched before")))
                .toList());
    }

}
