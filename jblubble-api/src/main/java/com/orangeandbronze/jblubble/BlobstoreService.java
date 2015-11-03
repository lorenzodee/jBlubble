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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Strategy interface for the creation and serving of large binary objects (or
 * BLOBs). The BLOBs are intended to be immutable. Once created, the BLOB cannot
 * be changed. To change, a new BLOB should be created, reference the new one,
 * and possibly delete the old one.
 *
 * <h3>Some Sample Use Cases</h3>
 * <p>
 * When handling file uploads in servlets (3.0 and above), this is how the
 * service can be used:
 * </p>
 * 
 * <pre>
 * // HttpServletRequest
 * request.getPart("...").write(fileName);
 * // Open an input stream with the file (created from uploaded part)
 * InputStream in = new FileInputStream(fileName);
 * ... blobKey = blobstoreService.createBlob(in, ...);
 * </pre>
 * 
 * <pre>
 * // HttpServletResponse
 * BlobInfo blobInfo = blobstoreService.getBlobInfo(blobKey);
 * response.setContentType(blobInfo.getContentType());
 * OutputStream out = response.getOutputStream();
 * blobstoreService.serveBlob(blobKey, out);
 * </pre>
 * <p>
 * When using Spring MVC, this is how the service can be used inside a
 * controller:
 * </p>
 * 
 * <pre>
 * // MultipartFile
 * ... blobKey = blobstoreService.createBlob(
 *     multipartFile.getInputStream(), multipartFile.getName(), ...);
 * </pre>
 *
 * @author Lorenzo Dee
 */
public interface BlobstoreService {

	/**
	 * Stores the blob and returns a unique identifier for later retrieval.
	 * <p>
	 * This method expects the blob contents coming from an {@link InputStream}.
	 * The caller is responsible for closing this {@link InputStream input stream}.
	 *
	 * @param in
	 *            the given blob
	 * @param name
	 *            the name
	 * @param contentType
	 *            the MIME type of the given blob (e.g. image/png,
	 *            application/pdf)
	 * @return the unique identifier to retrieve the blob
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws BlobstoreException
	 *             when an error occurs while storing the blob
	 */
	BlobKey createBlob(InputStream in,
			String name, String contentType)
			throws IOException, BlobstoreException;

	/**
	 * Stores the blob and returns a unique identifier for later retrieval.
	 * <p>
	 * This method expects the blob contents to be written to an
	 * {@link OutputStream} via a call-back interface.
	 * 
	 * @param callback
	 *            the call-back interface to write to output stream
	 * @param name
	 *            the name
	 * @param contentType
	 *            the MIME type of the given blob (e.g. image/png,
	 *            application/pdf)
	 * @return the unique identifier to retrieve the blob
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws BlobstoreException
	 *             when an error occurs while storing the blob
	 */
	BlobKey createBlob(
			BlobstoreWriteCallback callback, String name,
			String contentType)
			throws IOException, BlobstoreException;

	/**
	 * Returns metadata about a blob with the given identifier. Returns
	 * <code>null</code> if no blob exists with the given identifier.
	 * <p>
	 * This <em>does not</em> return the contents of the blob. See
	 * {@link #serveBlob(BlobKey, OutputStream)} for this.
	 *
	 * @param blobKey
	 *            the unique identifier
	 * @return metadata about a blob, or <code>null</code> if none exists
	 * @throws BlobstoreException
	 *             if an error occurs while retrieving the metadata of the blob
	 */
	BlobInfo getBlobInfo(BlobKey blobKey) throws BlobstoreException;

	/**
	 * Writes the blob with the given identifier to the given output stream.
	 * 
	 * @param blobKey
	 *            the unique identifier
	 * @param out
	 *            the output stream
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws BlobstoreException
	 *             if an error occurs while retrieving the blob (e.g. does not
	 *             exist)
	 */
	void serveBlob(BlobKey blobKey, OutputStream out)
			throws IOException, BlobstoreException;

	/**
	 * Writes the blob with the given identifier to the given output stream.
	 * This method serves a byte range of the blob.
	 * 
	 * @param blobKey
	 *            the unique identifier
	 * @param out
	 *            the output stream
	 * @param start
	 *            Start index of blob range to serve
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws BlobstoreException
	 *             if an error occurs while retrieving the blob (e.g. does not
	 *             exist)
	 * @since 1.1
	 */
	void serveBlob(BlobKey blobKey, OutputStream out, long start)
			throws IOException, BlobstoreException;

	/**
	 * Writes the blob with the given identifier to the given output stream.
	 * This method serves a byte range of the blob.
	 * 
	 * @param blobKey
	 *            the unique identifier
	 * @param out
	 *            the output stream
	 * @param start
	 *            Start index of byte range to serve
	 * @param end
	 *            End index of byte range to serve. Index is inclusive, meaning
	 *            the byte indicated by end is included in the output stream.
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws BlobstoreException
	 *             if an error occurs while retrieving the blob (e.g. does not
	 *             exist)
	 * @since 1.1
	 */
	void serveBlob(BlobKey blobKey, OutputStream out, long start, long end)
			throws IOException, BlobstoreException;

	/**
	 * Deletes the specified blobs.
	 * 
	 * @param blobKeys
	 *            the unique identifiers
	 * @return an array of update counts ordered according to the keys
	 * @throws BlobstoreException
	 *             if an error occurs while deleting the blobs
	 */
	int[] delete(BlobKey... blobKeys) throws BlobstoreException;

	/**
	 * Reads the blob with the given identifier. The blob contents are made
	 * available to the callback as an {@link InputStream input stream}.
	 *
	 * @param blobKey
	 *            the unique identifier
	 * @param callback
	 *            the call-back interface to read blob contents
	 * @throws IOException
	 *             if an I/O error occurred
	 * @throws BlobstoreException
	 *             if an error occurs while retrieving the blob (e.g. does not
	 *             exist)
	 * @since 1.1
	 */
	void readBlob(BlobKey blobKey, BlobstoreReadCallback callback)
			throws IOException, BlobstoreException;

}
