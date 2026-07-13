package io.github.belgif.rest.guide.validator.core.util;

import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.Reference;
import org.eclipse.microprofile.openapi.models.media.Schema;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CircularReferenceUtil {

    private CircularReferenceUtil() {
    }

    public static void validateCircularReferences(Parser.ParserResult result) {
        for (OpenApiDefinition<?> def : result.getAllDefinitions()) {
            if (containsUnsafeCycle(def, new ArrayList<>(), result)) {
                log.error("{} contains a circular reference to itself", def.getPrintableJsonPointer());  //TODO: not necessarily def anymore, but can be nested/referenced schema
                result.setParsingValid(false);
                return;
            }
        }
    }

    /**
     *
     * @param def - a definition referenced by the last definition in visited, e.g. D if C -> D
     * @param visited - chain of definitions referencing one another e.g.  A -> B -> C
     * @param result
     * @return  Whether there's an unsafe circular reference concerning 'def' or one of the definitions it references
     */
    private static boolean containsUnsafeCycle(OpenApiDefinition<?> def, List<Visit> visited, Parser.ParserResult result) {
        if (isUnsafeCycle(def, visited)) {
            return true;
        }
        if (visited.stream().anyMatch(r -> r.source().equals(def))) {
            // In case there is a legit circular reference, the infinite loop should stop.
            return false;
        }
        if (!(def.getModel() instanceof Reference)) {  //TODO: check if it's a type that can be a reference; why this check? instead of def.getModel() instanceof def.getClass()
            return false;
        }
        for (Ref ref : getOutgoingRefs(def, result)) {  // TODO: not always refs, can also be subschemas
            List<Visit> nextVisit = new ArrayList<>(visited);
            nextVisit.add(new Visit(def, ref.discriminator));

            if (containsUnsafeCycle(ref.target(), nextVisit, result)) {
                return true;
            }
        }
        return false;
    }

    private static List<Ref> getOutgoingRefs(OpenApiDefinition<?> definition, Parser.ParserResult result) {
        List<Ref> refs = new ArrayList<>();

        if (definition.hasReference()) {
            refs.add(new Ref(result.resolve(definition.getModel()), false));
        }

        if (definition instanceof SchemaDefinition schemaDefinition) {
            addDiscriminatorRefs(refs, schemaDefinition, result);
            addSchemaCollectionRefs(refs, schemaDefinition.getModel().getAllOf(), result);
            addSchemaCollectionRefs(refs, schemaDefinition.getModel().getOneOf(), result);
            addSchemaCollectionRefs(refs, schemaDefinition.getModel().getAnyOf(), result);

            if (schemaDefinition.getModel().getNot() != null) {
                refs.add(new Ref(result.resolve(schemaDefinition.getModel().getNot()), false));
            }
        }

        return refs;
    }

    private static boolean isUnsafeCycle(OpenApiDefinition<?> def, List<Visit> visited) {
        return visited.stream().anyMatch(r -> r.source().equals(def))
                && (
                visited.stream().allMatch(Visit::discriminator) ||
                        visited.stream().noneMatch(Visit::discriminator)
        );
    }


    private static void addSchemaCollectionRefs(List<Ref> refs, List<Schema> schemaDefinitionList, Parser.ParserResult result) {
        if (schemaDefinitionList == null || schemaDefinitionList.isEmpty()) {
            return;
        }
        refs.addAll(schemaDefinitionList.stream().map(result::resolve).map(r -> new Ref(r, false)).toList());
    }

    private static void addDiscriminatorRefs(List<Ref> refs, SchemaDefinition schemaDefinition, Parser.ParserResult result) {
        if (schemaDefinition.getModel().getDiscriminator() == null || schemaDefinition.getModel().getDiscriminator().getMapping() == null) {
            return;
        }
        refs.addAll(schemaDefinition.getModel().getDiscriminator().getMapping().values().stream()
                .map(m -> result.resolveDiscriminatorMapping(schemaDefinition, m).orElseThrow(() -> new IllegalStateException("Reference does not exist but somehow was not catched before")))
                .map(r -> new Ref(r, true))
                .toList());
    }

    private record Ref(
            OpenApiDefinition<?> target,
            boolean discriminator) {
    }

    private record Visit(OpenApiDefinition<?> source, boolean discriminator) {
    }

}
