package be.belgium.gcloud.rest.styleguide.validation.maven.junit;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.*;

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
    @XmlAttribute(name = "system-out")
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

}
