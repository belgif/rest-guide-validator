package io.github.belgif.rest.styleguide.validation;

import io.github.belgif.rest.styleguide.validation.core.OpenApiViolationAggregator;
import io.github.belgif.rest.styleguide.validation.core.Violation;
import io.github.belgif.rest.styleguide.validation.core.ViolationType;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConsoleOutputProcessor implements OutputProcessor {
    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {

        List<Violation> violations = violationAggregator.getViolations().stream().filter(v -> v.type != ViolationType.IGNORED).sorted().collect(Collectors.toList());
        List<Violation> ignored = violationAggregator.getViolations().stream().filter(v -> v.type == ViolationType.IGNORED).sorted().collect(Collectors.toList());

        System.out.printf("\n OpenApi validation summary: %d violations and %d ignored violations.\n", violations.size(), ignored.size());

        violations.forEach(v -> {
            if (Objects.requireNonNull(v.type) == ViolationType.MANDATORY) {
                System.err.println(v);
            } else {
                System.out.println(v);
            }
        });
        ignored.forEach(v -> System.out.println(v.toString()));
    }
}
