package eu.europeana.cloud.common.model.dps;

/**
 * Statistics of a node's attribute
 */
public class AttributeStatistics {
    /** Attribute name */
    private String name;

    /** Attribute value */
    private String value;

    /** Attribute value occurrence */
    private long occurrence;

    public AttributeStatistics(String name, String value) {
        this(name, value, 1);
    }

    public AttributeStatistics(String name, String value, long occurrence) {
        this.name = name;
        this.value = value;
        this.occurrence = occurrence <= 0 ? 1 : occurrence;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public long getOccurrence() {
        return occurrence;
    }

    public void increaseOccurrence(int count) {
        if (count > 0) {
            this.occurrence += count;
        }
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof AttributeStatistics)) {
            return false;
        }

        AttributeStatistics attributeStatistics = (AttributeStatistics) o;

        return attributeStatistics.getName().equals(name) &&
                attributeStatistics.getValue().equals(value);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + name.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }
}
