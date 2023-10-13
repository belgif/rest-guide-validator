package be.belgium.gcloud.rest.styleguide.validation.maven.junit;

import jakarta.xml.bind.annotation.*;
import lombok.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

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
    private Map<String, String> sysout = new HashMap<>();


    @XmlElement(name = "system-out")
    public String getSystemOut() {
        if (sysout == null)
            return "";
        return sysout.keySet().stream().sorted()
                .map(k -> "Line " + k + ": " + sysout.get(k))
                .collect(Collectors.joining("\n"));

    }

    public void appendSysOut(String fileName, int line, String msg) {
        if (sysout == null)
            sysout = new HashMap<>();
        sysout.put(fileName + ":" + line, msg != null ? msg : " -- no additional info --");

    }

}
