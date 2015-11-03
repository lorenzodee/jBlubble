/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orangeandbronze.jblubble;

import java.util.Date;

/**
 * Contains metadata about a blob. This metadata is usually gathered by parsing
 * the HTTP headers included in the blob upload.
 *
 * @author Lorenzo Dee
 */
public class BlobInfo {

	private final BlobKey blobKey;
	private final String name;
	private final String contentType;
	private final long size;
	private final Date dateCreated;
	private final String md5Hash;

	public BlobInfo(BlobKey blobKey, String name, String contentType,
			long size, Date dateCreated, String md5Hash) {
		super();
		this.blobKey = blobKey;
		this.name = name;
		this.contentType = contentType;
		this.size = size;
		this.dateCreated = dateCreated;
		this.md5Hash = md5Hash;
	}

	public BlobKey getBlobKey() {
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

	/**
	 * @since 1.1
	 */
	public String getMd5Hash() {
		return md5Hash;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((blobKey == null) ? 0 : blobKey.hashCode());
		result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
		result = prime * result + ((dateCreated == null) ? 0 : dateCreated.hashCode());
		result = prime * result + ((md5Hash == null) ? 0 : md5Hash.hashCode());
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
		if (md5Hash == null) {
			if (other.md5Hash != null)
				return false;
		} else if (!md5Hash.equals(other.md5Hash))
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
