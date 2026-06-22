package io.github.belgif.rest.guide.validator.core.util;

import io.github.belgif.rest.guide.validator.core.model.OpenApiDefinition;
import io.github.belgif.rest.guide.validator.core.model.SchemaDefinition;
import io.github.belgif.rest.guide.validator.core.parser.Parser;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class CircularReferenceUtil {

    private CircularReferenceUtil() {
    }

    private static final Set<RefType> SAFE_REFS = EnumSet.of(
            RefType.PROPERTY,
            RefType.ITEMS,
            RefType.ADDITIONAL_PROPERTIES,
            RefType.DISCRIMINATOR //Not considered safe when it's the only reference.
    );

    public static void validateCircularReferences(Parser.ParserResult result) {
        for (OpenApiDefinition<?> def : result.getAllDefinitions()) {
            if (containsUnsafeCycle(def, new ArrayList<>(), new ArrayList<>())) {
                log.error("{} contains a circular reference to itself", def.getPrintableJsonPointer());
                result.setParsingValid(false);
                return;
            }
        }
    }

    private static boolean containsUnsafeCycle(OpenApiDefinition<?> current, List<OpenApiDefinition<?>> visited, List<RefType> path) {
        int cycleStart = visited.indexOf(current);
        if (cycleStart >= 0) {
            List<RefType> cyclePath = path.subList(cycleStart, path.size());

            return isUnsafe(cyclePath);
        }

        visited.add(current);

        for (OpenApiDefinition<?> ref : current.getReferencedBy()) {
            RefType type = getRefType(ref);
            path.add(type);

            if (containsUnsafeCycle(getDefinitionRoot(ref), visited, path)) {
                return true;
            }

            path.remove(path.size() - 1);
        }

        visited.remove(visited.size() - 1);
        return false;
    }

    /*
    Finds definition that references something, so not for example an index of a oneOf.
     */
    private static OpenApiDefinition<?> getDefinitionRoot(OpenApiDefinition<?> ref) {
        if (ref.getDefinitionType() == OpenApiDefinition.DefinitionType.TOP_LEVEL || !ref.getParent().getClass().isInstance(ref)) {
            return ref;
        }
        if (ref instanceof SchemaDefinition schemaRef && schemaRef.isInlineSchemaOfProperty()) {
            return schemaRef;
        }
        return getDefinitionRoot(ref.getParent());
    }

    private static boolean isUnsafe(List<RefType> refTypes) {
        return refTypes.stream().noneMatch(SAFE_REFS::contains) ||
                (refTypes.size() == 1 && refTypes.get(0) == RefType.DISCRIMINATOR);
    }

    private static RefType getRefType(OpenApiDefinition<?> ref) {
        // Might need something here with topLevelOfSameType or something like that...
        int index = getDefinitionRoot(ref).getJsonPointer().splitSegments().size();
        List<String> segments = ref.getJsonPointer().splitSegments().subList(index, ref.getJsonPointer().splitSegments().size());
        // direct ref or discriminator
        if (segments.isEmpty()) {
            if (ref.hasReference()) {
                return RefType.REF;
            }
            if (ref instanceof SchemaDefinition schemaRef && schemaRef.getModel().getDiscriminator() != null) {
                return RefType.DISCRIMINATOR;
            }
            throw new IllegalStateException("No valid reference type found.");
        }
        int indexToUse = useLastSegment(segments, ref) ? segments.size() - 1 : segments.size() - 2;
        return RefType.fromSegment(segments.get(indexToUse));
    }

    private static boolean useLastSegment(List<String> segments, OpenApiDefinition<?> ref) {
        String lastSegment = segments.get(segments.size() - 1);
        if (!lastSegment.equals(RefType.ITEMS.getSegment()) && !lastSegment.equals(RefType.ADDITIONAL_PROPERTIES.getSegment())) {
            return false;
        }
        if (lastSegment.equals(RefType.ITEMS.getSegment()) && ref.getParent() != null && ref.getParent() instanceof SchemaDefinition schema && schema.getModel().getItems() != null) {
            return true;
        }
        return lastSegment.equals(RefType.ADDITIONAL_PROPERTIES.getSegment()) && ref.getParent() != null && ref.getParent() instanceof SchemaDefinition schema && schema.getModel().getAdditionalPropertiesSchema() != null;
    }

    @Getter
    private enum RefType {
        REF(""),
        ALL_OF("allOf"),
        ONE_OF("oneOf"),
        ANY_OF("anyOf"),
        NOT("not"),
        DISCRIMINATOR(""),
        PROPERTY("properties"),
        ITEMS("items"),
        ADDITIONAL_PROPERTIES("additionalProperties");

        private final String segment;

        RefType(String segment) {
            this.segment = segment;
        }

        public static RefType fromSegment(String segment) {
            for (RefType value : values()) {
                if (value.segment.equals(segment)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Unknown segment: " + segment);
        }
    }

}
