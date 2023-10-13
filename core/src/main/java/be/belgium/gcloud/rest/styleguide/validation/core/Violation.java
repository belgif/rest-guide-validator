package be.belgium.gcloud.rest.styleguide.validation.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Violation implements Comparable<Violation> {
    String ruleName;
    String message;
    public ViolationType type;
    Line lineNumber;

    @Override
    public String toString() {
        return (lineNumber.getLineNumber() > 0 ? String.format("file: %13s: ln%4d:", lineNumber.getFileName(), lineNumber.getLineNumber()) : "") +
                String.format("  %-14S ", ("[" + type + "]")) +
                String.format("%-17s ", ruleName) +
                message;
    }

    @Override
    public int compareTo(Violation o) {
        if (lineNumber.getFileName().equals(o.lineNumber.getFileName())) {
            return Integer.compare(lineNumber.getLineNumber(), o.lineNumber.getLineNumber());
        } else {
            return 0;
        }
    }
}
