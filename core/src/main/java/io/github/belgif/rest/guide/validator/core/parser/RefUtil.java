package io.github.belgif.rest.guide.validator.core.parser;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class RefUtil {

    private static final String EXAMPLES = "examples";
    private static final String EXAMPLE = "example";
    private static final String PROPERTIES = "properties";

    private RefUtil() {
    }

    /**
     * @param refNode The jsonNode containing the reference
     * @param rootNode The rootNode of the json/yaml document
     * @return true if the referenced node is on a location in the root document where references are allowed. Optional will be emptu if the refNode was not found in rootNode.
     */
    public static Optional<Boolean> isRefInValidLocation(JsonNode refNode, JsonNode rootNode) {
        Optional<List<String>> path = findPathToNode(refNode, rootNode, new ArrayList<>());
        return path.map(RefUtil::isValidPath);
    }

    private static boolean isValidPath(List<String> path) {
        if (path.isEmpty()) return false;
        if (path.contains(EXAMPLES)) {
            // A $ref is allowed directly in an example object under examples. In a single example a $ref is not allowed.
            return path.indexOf(EXAMPLES) == path.size() - 2;
        }
        return !path.contains(EXAMPLE) && !path.get(path.size() - 1).equals(PROPERTIES);
    }

    /**
     * @param refNode The jsonNode containing the reference
     * @param rootNode The rootNode of the json/yaml document
     * @return list of property keys how to get from root to the node containing the reference.
     */
    private static Optional<List<String>> findPathToNode(JsonNode refNode, JsonNode rootNode, List<String> path) {
        if (rootNode == refNode) {
            return Optional.of(List.copyOf(path));
        }
        if (rootNode.isObject()) {
            for (Map.Entry<String, JsonNode> entry : rootNode.properties()) {
                path.add(entry.getKey());
                Optional<List<String>> found = findPathToNode(refNode, entry.getValue(), path);
                if (found.isPresent()) {
                    return found;
                }
                path.remove(path.size() - 1);
            }
        } else if (rootNode.isArray()) {
            for (int i = 0; i < rootNode.size(); i++) {
                path.add(String.valueOf(i));
                Optional<List<String>> found = findPathToNode(refNode, rootNode.get(i), path);
                if (found.isPresent()) {
                    return found;
                }
                path.remove(path.size() - 1);
            }
        }
        return Optional.empty();
    }
}