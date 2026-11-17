package io.github.belgif.rest.guide.validator.runner.output.junit;

import jakarta.xml.bind.annotation.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Generated class to write a XML output file (junit)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@XmlAccessorType(XmlAccessType.FIELD)
public class Testcase {
    @XmlAttribute
    private String classname;
    @XmlAttribute
    private String name;
    @XmlAttribute
    private String assertions;
    @XmlAttribute
    private String time;
    @XmlAttribute
    private String status;
    @XmlAttribute
    private String skipped;

    @XmlElement(name = "system-err")
    private String systemErr;

    Failure failure;
    Error error;

    @XmlTransient
    private final List<String> sysout = new ArrayList<>();


    @XmlElement(name = "system-out")
    public String getSystemOut() {
        return String.join("\n", sysout);
    }

    public void appendSysOut(String violationMessage) {
        sysout.add(violationMessage);
    }

}
