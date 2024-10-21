package io.github.belgif.rest.guide.validator.output.junit;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

/**
 * Generated class to write a XML output file (junit)
 */
@XmlRootElement(name = "testsuite")
public class Testsuite {
    private String id;
    private String name; //mandatory
    private String pkg;
    private String hostname;
    private String timestamp;

    private int tests; //mandatory
    private int failures;
    private int disabled;
    private float time;
    private int errors;
    private int skipped;

    private String systemErr;
    private String systemOut;

    private List<Testcase> testcase = new ArrayList<>();

    @XmlAttribute
    public String getFailures() {
        return String.valueOf(failures);
    }

    @XmlAttribute(required = true)
    public String getTests() {
        return String.valueOf(tests);
    }

    @XmlAttribute(name = "package")
    public String getPkg() {
        return pkg;
    }

    @XmlAttribute
    public String getSkipped() {
        return String.valueOf(skipped);
    }

    @XmlAttribute(name = "system-out")
    public String getSystemOut() {
        return systemOut;
    }

    @XmlAttribute
    public String getHostname() {
        return hostname;
    }

    @XmlAttribute(required = true)
    public String getName() {
        return name;
    }

    @XmlAttribute(name = "system-err")
    public String getSystemErr() {
        return systemErr;
    }

    @XmlAttribute
    public String getDisabled() {
        return String.valueOf(disabled);
    }

    @XmlAttribute
    public String getId() {
        return id;
    }

    @XmlAttribute
    public String getErrors() {
        return String.valueOf(errors);
    }

    @XmlAttribute
    public String getTimestamp() {
        return timestamp;
    }

    @XmlAttribute
    public String getTime(){
        return String.valueOf(time);
    }


    public List<Testcase> getTestcase() {
        if(testcase == null)
            testcase = new ArrayList<>();
        return testcase;
    }

    public void addTestcase(Testcase testcase){
        if(this.testcase == null) {
            this.testcase = new ArrayList<>();
        }
        this.testcase.add(testcase);
    }
}
