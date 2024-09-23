package io.github.belgif.rest.guide.validator.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.belgif.rest.guide.validator.core.OpenApiViolationAggregator;
import io.github.belgif.rest.guide.validator.core.Violation;
import io.github.belgif.rest.guide.validator.output.model.ViolationFileObject;
import io.github.belgif.rest.guide.validator.output.model.ViolationGroup;
import io.github.belgif.rest.guide.validator.output.model.ViolationReport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonOutputProcessor extends OutputProcessor implements DirectoryOutputProcessor {
    private static final String FILE_NAME = "validationReport.json";
    /**
     * Output directory
     */
    private File output;

    public JsonOutputProcessor(OutputGroupBy outputGroupBy) {
        super(outputGroupBy);
    }

    @Override
    public void setOutput(File outputFile) {
        this.output = outputFile;
    }

    @Override
    public void process(OpenApiViolationAggregator violationAggregator) {
        writeToFile(mapToViolationReport(violationAggregator));
    }

    private ViolationReport mapToViolationReport(OpenApiViolationAggregator violationAggregator) {
        var violations = this.getOutputGroupBy().groupViolations(violationAggregator.getViolations());
        List<ViolationGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<Violation>> entry : violations.entrySet()) {
            groups.add(new ViolationGroup(
                    getOutputGroupBy().getIdentifier(entry.getValue().get(0)),
                    entry.getValue().size(),
                    entry.getValue().stream().map(this::mapToViolationFileObject).toList()));
        }
        return new ViolationReport(
                violationAggregator.getAmountOfActionableViolations(),
                violationAggregator.getAmountOfIgnoredViolations(),
                this.getOutputGroupBy().name(),
                groups
        );
    }

    private ViolationFileObject mapToViolationFileObject(Violation violation) {
        return new ViolationFileObject(
                violation.getRuleName(),
                violation.getDescription(),
                violation.getMessage(),
                violation.getType().toString(),
                violation.getLineNumber().getFileName(),
                violation.getLineNumber().getLineNumber(),
                violation.getPointer());
    }

    private void writeToFile(ViolationReport violationReport) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            mapper.writeValue(new File(output, FILE_NAME), violationReport);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
