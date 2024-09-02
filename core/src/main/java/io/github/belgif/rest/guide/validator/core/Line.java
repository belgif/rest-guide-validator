package io.github.belgif.rest.guide.validator.core;

import lombok.Getter;

@Getter
public class Line {

    private final String fileName;
    private final int lineNumber;

    public Line(String fileName, int lineNumber) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    @Override
    public String toString() {
        return fileName + " - ln: " + lineNumber;
    }
}
