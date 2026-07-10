package io.github.belgif.rest.guide.validator.core.util;

import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.openapi.models.Reference;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CircularReferenceUtil {

    private CircularReferenceUtil() {
    }

    public static void validateCircularReferences(Parser.ParserResult result) {
        for (OpenApiDefinition<?> def : result.getAllDefinitions()) {
            if (containsUnsafeCycle(def, new ArrayList<>(), result)) {
                log.error("{} contains a circular reference to itself", def.getPrintableJsonPointer());
                result.setParsingValid(false);
                return;
            }
        }
    }

    private static boolean containsUnsafeCycle(OpenApiDefinition<?> def, List<Ref> visited, Parser.ParserResult result) {
        if (visitedContainsDefinitionViaUnsafeRef(def, visited)) {
            return true;
        }
        if (visited.stream().anyMatch(r -> r.refFrom().equals(def))) {
            // In case there is a legit circular reference, the infinite loop should stop.
            return false;
        }
        if (!(def.getModel() instanceof Reference)) {
            return false;
        }
        if (def instanceof SchemaDefinition schemaDefinition) {
            // Return if one of these options is true:
            return directRefContainsUnsafeCycle(def, visited, result) ||
                    discriminatorRefContainsUnsafeCycle(schemaDefinition, visited, result) ||
                    allOfRefContainsUnsafeCycle(schemaDefinition, visited, result) ||
                    oneOfRefContainsUnsafeCycle(schemaDefinition, visited, result) ||
                    anyOfRefContainsUnsafeCycle(schemaDefinition, visited, result) ||
                    notRefContainsUnsafeCycle(schemaDefinition, visited, result);
        } else {
            return directRefContainsUnsafeCycle(def, visited, result);
        }
    }

    private static boolean visitedContainsDefinitionViaUnsafeRef(OpenApiDefinition<?> def, List<Ref> visited) {
        return visited.stream().anyMatch(r -> r.refFrom().equals(def))
                && (
                visited.stream().allMatch(Ref::viaDiscriminator) ||
                        visited.stream().noneMatch(Ref::viaDiscriminator)
        );
    }

    private static boolean notRefContainsUnsafeCycle(SchemaDefinition schemaDefinition, List<Ref> visited, Parser.ParserResult result) {
        if (schemaDefinition.getModel().getNot() == null) {
            return false;
        }
        OpenApiDefinition<?> referenced = result.resolve(schemaDefinition.getModel().getNot());
        return callNextCycle(schemaDefinition, referenced, false, visited, result);
    }

    private static boolean anyOfRefContainsUnsafeCycle(SchemaDefinition schemaDefinition, List<Ref> visited, Parser.ParserResult result) {
        if (schemaDefinition.getModel().getAnyOf() == null || schemaDefinition.getModel().getAnyOf().isEmpty()) {
            return false;
        }
        return schemaDefinition.getModel().getAnyOf().stream()
                .map(result::resolve)
                .anyMatch(r -> callNextCycle(schemaDefinition, r, false, visited, result));
    }


    private static boolean oneOfRefContainsUnsafeCycle(SchemaDefinition schemaDefinition, List<Ref> visited, Parser.ParserResult result) {
        if (schemaDefinition.getModel().getOneOf() == null || schemaDefinition.getModel().getOneOf().isEmpty()) {
            return false;
        }
        return schemaDefinition.getModel().getOneOf().stream()
                .map(result::resolve)
                .anyMatch(r -> callNextCycle(schemaDefinition, r, false, visited, result));
    }

    private static boolean allOfRefContainsUnsafeCycle(SchemaDefinition schemaDefinition, List<Ref> visited, Parser.ParserResult result) {
        if (schemaDefinition.getModel().getAllOf() == null || schemaDefinition.getModel().getAllOf().isEmpty()) {
            return false;
        }
        return schemaDefinition.getModel().getAllOf().stream()
                .map(result::resolve)
                .anyMatch(r -> callNextCycle(schemaDefinition, r, false, visited, result));
    }

    private static boolean discriminatorRefContainsUnsafeCycle(SchemaDefinition schemaDefinition, List<Ref> visited, Parser.ParserResult result) {
        if (schemaDefinition.getModel().getDiscriminator() == null || schemaDefinition.getModel().getDiscriminator().getMapping() == null) {
            return false;
        }
        return schemaDefinition.getModel().getDiscriminator().getMapping().values().stream()
                .map(m -> result.resolveDiscriminatorMapping(schemaDefinition, m).orElseThrow(() -> new IllegalStateException("Reference does not exist but somehow was not catched before")))
                .anyMatch(r -> callNextCycle(schemaDefinition, r, true, visited, result));
    }

    private static boolean directRefContainsUnsafeCycle(OpenApiDefinition<?> def, List<Ref> visited, Parser.ParserResult result) {
        if (!def.hasReference()) {
            return false;
        }
        OpenApiDefinition<?> referenced = result.resolve(def.getModel());
        return callNextCycle(def, referenced, false, visited, result);
    }

    private static boolean callNextCycle(OpenApiDefinition<?> def, OpenApiDefinition<?> referenced, boolean viaDiscriminator, List<Ref> visited, Parser.ParserResult result) {
        List<Ref> branchedVisited = new ArrayList<>(visited);
        branchedVisited.add(new Ref(def, viaDiscriminator));
        return containsUnsafeCycle(referenced, branchedVisited, result);
    }

    private record Ref(OpenApiDefinition<?> refFrom, boolean viaDiscriminator) {
    }

}
