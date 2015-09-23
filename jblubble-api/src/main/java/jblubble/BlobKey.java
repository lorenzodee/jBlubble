package jblubble;

import java.io.Serializable;

@SuppressWarnings("serial")
public class BlobKey implements Serializable {

	private final String value;
	
	public BlobKey(String value) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(
					"Value cannot be null or empty");
		}
		this.value = value;
	}

	public String stringValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BlobKey other = (BlobKey) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value;
	}

}
