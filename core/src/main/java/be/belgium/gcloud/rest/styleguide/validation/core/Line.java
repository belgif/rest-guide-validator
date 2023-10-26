package be.belgium.gcloud.rest.styleguide.validation.core;

import lombok.Getter;

@Getter
public class Line {

    private final String fileName;
    private final int lineNumber;

    public Line(String fileName, int lineNumber) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

}
