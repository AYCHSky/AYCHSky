package eu.europeana.cloud.common.model.dps;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;

/**
 * Created by Tarek on 2/4/2019.
 */

@XmlRootElement()
public class NodeReport {
    private String nodeValue;

    private long occurrence;
    private List<AttributeStatistics> attributeStatistics;

    public List<AttributeStatistics> getAttributeStatistics() {
        return attributeStatistics;
    }

    public void setAttributeStatistics(List<AttributeStatistics> attributeStatistics) {
        this.attributeStatistics = attributeStatistics;
    }

    public NodeReport(String nodeValue, long occurrence, List<AttributeStatistics> attributeStatistics) {
        this.nodeValue = nodeValue;
        this.occurrence = occurrence;
        this.attributeStatistics = attributeStatistics;
    }

    public NodeReport() {
    }

    public String getNodeValue() {
        return nodeValue;
    }

    public void setNodeValue(String nodeValue) {
        this.nodeValue = nodeValue;
    }

    public long getOccurrence() {
        return occurrence;
    }

    public void setOccurrence(long occurrence) {
        this.occurrence = occurrence;
    }


}