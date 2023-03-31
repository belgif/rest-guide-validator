package be.belgium.gcloud.rest.styleguide.validation.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class Violation implements Comparable<Violation>{
    String ruleName;
    String message;
    public ViolationType type;
    int lineNumber;

    @Override
    public String toString() {
        return  (lineNumber > 0 ? String.format("lineNumber:%6d", lineNumber) : "")+
                String.format("  %-14S ", ("[" + type + "]")) +
                String.format("%-28s ", ruleName) +
                message ;
    }

    @Override
    public int compareTo(Violation o) {
        return Integer.compare(lineNumber, o.lineNumber);
    }
}
