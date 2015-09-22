package jblubble;

import java.util.Date;

/**
 * Contains metadata about a blob. This metadata is usually gathered by parsing
 * the HTTP headers included in the blob upload.
 *
 */
public class BlobInfo {

	private final String blobKey;
	private final String name;
	private final String contentType;
	private final long size;
	private final Date dateCreated;

	public BlobInfo(String blobKey, String name, String contentType,
			long size, Date dateCreated) {
		super();
		this.blobKey = blobKey;
		this.name = name;
		this.contentType = contentType;
		this.size = size;
		this.dateCreated = dateCreated;
	}

	public String getBlobKey() {
		return blobKey;
	}

	public String getName() {
		return name;
	}

	public String getContentType() {
		return contentType;
	}

	public long getSize() {
		return size;
	}

	public Date getDateCreated() {
		return dateCreated;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((blobKey == null) ? 0 : blobKey.hashCode());
		result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result + ((dateCreated == null) ? 0 : dateCreated.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (int) (size ^ (size >>> 32));
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
		BlobInfo other = (BlobInfo) obj;
		if (blobKey == null) {
			if (other.blobKey != null)
				return false;
		} else if (!blobKey.equals(other.blobKey))
			return false;
		if (contentType == null) {
			if (other.contentType != null)
				return false;
		} else if (!contentType.equals(other.contentType))
			return false;
		if (dateCreated == null) {
			if (other.dateCreated != null)
				return false;
		} else if (!dateCreated.equals(other.dateCreated))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

}
