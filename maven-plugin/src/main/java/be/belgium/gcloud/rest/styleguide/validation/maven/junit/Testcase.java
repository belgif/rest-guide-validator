package be.belgium.gcloud.rest.styleguide.validation.maven.junit;

import be.belgium.gcloud.rest.styleguide.validation.core.Violation;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.*;

import java.util.Objects;

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
    @XmlElement(name = "system-out")
    private String systemOut;
    @XmlAttribute
    private String assertions;
    @XmlAttribute
    private String time;
    @XmlAttribute
    private String status;
    @XmlAttribute
    private String skipped;
    @XmlAttribute(name = "system-err")
    private String systemErr;

    public Failure failure;
    public Error error;

    public void addMSysOut(String msg){
        if(systemOut != null)
            systemOut += "\n" + msg;
        else systemOut = msg;
    }

}
