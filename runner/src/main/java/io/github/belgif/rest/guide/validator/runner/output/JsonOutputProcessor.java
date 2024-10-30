package io.github.belgif.rest.guide.validator.runner.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.belgif.rest.guide.validator.core.ViolationReport;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.runner.output.model.ViolationEntry;
import io.github.belgif.rest.guide.validator.runner.output.model.ViolationGroup;
import io.github.belgif.rest.guide.validator.runner.output.model.OutputViolationReport;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonOutputProcessor extends OutputProcessor {
    private final File jsonOutputFile;

    public JsonOutputProcessor(OutputGroupBy outputGroupBy, File jsonOutputFile) {
        super(outputGroupBy);
        this.jsonOutputFile = jsonOutputFile;
    }

    @Override
    public void process(ViolationReport violationReport) {
        writeToFile(mapToViolationReport(violationReport));
    }

    private OutputViolationReport mapToViolationReport(ViolationReport violationAggregator) {
        var violations = this.getOutputGroupBy().groupViolations(violationAggregator.getViolations());
        LinkedHashMap<String, ViolationGroup> groups = new LinkedHashMap<>();
        for (Map.Entry<String, List<Violation>> entry : violations.entrySet()) {
            groups.put(getOutputGroupBy().getIdentifier(entry.getValue().get(0)),
                    new ViolationGroup(
                            entry.getValue().size(),
                            entry.getValue().stream().map(this::mapToViolationFileObject).toList()));
        }
        return new OutputViolationReport(
                violationAggregator.getAmountOfActionableViolations(),
                violationAggregator.getAmountOfIgnoredViolations(),
                this.getOutputGroupBy().value,
                groups
        );
    }

    private ViolationEntry mapToViolationFileObject(Violation violation) {
        return new ViolationEntry(
                violation.getRuleId(),
                violation.getDescription(),
                violation.getMessage(),
                violation.getLevel().value,
                violation.getLineNumber().getFileName(),
                violation.getLineNumber().getLineNumber(),
                "#" + violation.getPointer());
    }

    private void writeToFile(OutputViolationReport outputViolationReport) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            mapper.writeValue(jsonOutputFile, outputViolationReport);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
