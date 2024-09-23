package io.github.belgif.rest.guide.validator.output;

import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.core.ViolationType;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum OutputGroupBy {
    RULE("rule") {
        @Override
        public Map<String, List<Violation>> groupViolations(List<Violation> violations) {
            return collectToGroupedViolations(violations.stream().sorted(
                            Comparator.comparing(Violation::getType)
                                    .thenComparing(Violation::getRuleId)
                                    .thenComparing(Comparator.naturalOrder())
                    )
                    .collect(Collectors.groupingBy(
                            violation -> (violation.getRuleId() + violation.getType().toString() + violation.getDescription()),
                            LinkedHashMap::new,
                            Collectors.toList()
                    )));
        }

        @Override
        public String getIdentifier(Violation violation) {
            return violation.getRuleId();
        }


        @Override
        protected String getGroupLine(List<Violation> violations) {
            var violation = violations.get(0);
            return String.format("%-14S ", ("[" + violation.getType() + "]")) +
                    String.format("%-12s ", violation.getRuleId()) + violation.getDescription();
        }

        @Override
        protected void addReportMessages(List<Violation> violations) {
            for (Violation violation : violations) {
                var reportMessage = (violation.getLineNumber().getLineNumber() > 0 ? String.format("%-15s ln%4d  ", violation.getLineNumber().getFileName(), violation.getLineNumber().getLineNumber()) : "") +
                        (violation.getLineNumber().getLineNumber() == 0 && violation.getType().equals(ViolationType.IGNORED) ? String.format("%-15s ", violation.getLineNumber().getFileName()) : "") +
                        String.format("%s", "#" + violation.getPointer()) +
                        violation.getFormattedMessage();
                violation.setReportMessage(reportMessage);
            }
        }
    },
    FILE("file") {
        @Override
        public Map<String, List<Violation>> groupViolations(List<Violation> violations) {
            return collectToGroupedViolations(violations.stream().sorted()
                    .collect(Collectors.groupingBy(
                            v -> v.getLineNumber().getFileName(), LinkedHashMap::new, Collectors.toList()
                    )));
        }

        @Override
        public String getIdentifier(Violation violation) {
            return violation.getLineNumber().getFileName();
        }

        @Override
        protected String getGroupLine(List<Violation> violations) {
            var violation = violations.get(0);
            return String.format("File: %14s", ("[" + violation.getLineNumber().getFileName() + "]"));
        }

        @Override
        protected void addReportMessages(List<Violation> violations) {
            for (Violation violation : violations) {
                var reportMessage = String.format("%-14S ", ("[" + violation.getType() + "]")) +
                        String.format("%-14s ", violation.getRuleId()) +
                        (violation.getLineNumber().getLineNumber() > 0 ? String.format(" ln%4d  ", violation.getLineNumber().getLineNumber()) : "") +
                        String.format("%s%n", "#" + violation.getPointer()) +
                        violation.getDescription() +
                        violation.getFormattedMessage();
                violation.setReportMessage(reportMessage);
            }
        }
    };

    public final String value;

    OutputGroupBy(String value) {
        this.value = value;
    }

    // Should return a map with key: Description of group, and value a list of Violations with their reportMessage set.
    public abstract Map<String, List<Violation>> groupViolations(List<Violation> violations);

    public abstract String getIdentifier(Violation violation);

    protected abstract String getGroupLine(List<Violation> violation);

    protected abstract void addReportMessages(List<Violation> violations);

    protected Map<String, List<Violation>> collectToGroupedViolations(LinkedHashMap<String, List<Violation>> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> {
                            addReportMessages(entry.getValue());
                            return getGroupLine(entry.getValue());
                        },
                        Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new
                ));
    }

    public static OutputGroupBy fromString(String value) {
        for (OutputGroupBy outputGroupBy : OutputGroupBy.values()) {
            if (outputGroupBy.name().equalsIgnoreCase(value)) {
                return outputGroupBy;
            }
        }
        throw new IllegalArgumentException("No groupBy constant found for " + value);
    }
}
