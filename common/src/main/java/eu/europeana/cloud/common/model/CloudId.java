package eu.europeana.cloud.common.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Unique Identifier model
 * 
 * @author Yorgos.Mamakis@ kb.nl
 * 
 */
@XmlRootElement
public class CloudId {
	/**
	 * The unique identifier
	 */
	private String id;

	/**
	 * A providerId/recordId combo
	 */
	private LocalId localId;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public LocalId getLocalId() {
		return localId;
	}

	public void setLocalId(LocalId localId) {
		this.localId = localId;
	}

	@Override
	public boolean equals(Object e) {
		if (!e.getClass().isAssignableFrom(CloudId.class)) {
			return false;
		}

		if (!(this.id.contentEquals(((CloudId) e).getId()) && this.localId.equals(((CloudId) e).getLocalId()))) {
			return false;
		}
		return true;
	}
}