package jblubble;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Manage the creation and serving of large binary objects.
 * <p>
 * This is intended to avoid having to use <code>byte[]</code> (byte arrays)
 * that would otherwise cause {@link OutOfMemoryError out of memory errors} in
 * some cases.
 * <p>
 * When handling file uploads in {@linkplain servlets} (3.0 and above), this
 * is how the service can be used:
 * 
 * <pre>
 * // HttpServletRequest
 * request.getPart("...").write(fileName);
 * // Open an input stream with the file (created from uploaded part)
 * InputStream in = new FileInputStream(fileName);
 * String blobKey = blobstoreService.createBlob(in, ...);
 * </pre>
 * <p>
 * <pre>
 * // HttpServletResponse
 * BlobInfo blobInfo = blobstoreService.getBlobInfo(blobKey);
 * response.setContentType(blobInfo.getContentType());
 * OutputStream out = response.getOutputStream();
 * blobstoreService.serveBlob(blobKey, out);
 * </pre>
 * <p>
 * When using Spring MVC, this is how the service can be used inside a controller:
 * <pre>
 * // MultipartFile
 * String blobKey = blobstoreService.createBlob(
 *     multipartFile.getInputStream(), multipartFile.getName(), ...);
 * </pre>
 */
public interface BlobstoreService {

	/**
	 * Stores the blob and returns a unique identifier for later retrieval.
	 * <p>
	 * This method expects the blob contents coming from an {@link InputStream}.
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
	String createBlob(InputStream in,
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
	String createBlob(
			BlobstoreWriteCallback callback, String name,
			String contentType)
			throws IOException, BlobstoreException;

	/**
	 * Returns metadata about a blob with the given identifier. Returns
	 * <code>null</code> if no blob exists with the given identifier.
	 * <p>
	 * This <em>does not</em> return the contents of the blob. See
	 * {@link #serveBlob(String, OutputStream)} for this.
	 *
	 * @param blobKey
	 *            the unique identifier
	 * @return metadata about a blob, or <code>null</code> if none exists
	 * @throws BlobstoreException
	 *             if an error occurs while retrieving the metadata of the blob
	 */
	BlobInfo getBlobInfo(String blobKey) throws BlobstoreException;

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
	void serveBlob(String blobKey, OutputStream out) throws IOException, BlobstoreException;

	/**
	 * Deletes the specified blobs.
	 * 
	 * @param blobKeys
	 *            the unique identifiers
	 * @return an array of update counts ordered according to the keys
	 * @throws BlobstoreException
	 *             if an error occurs while deleting the blobs
	 */
	int[] delete(String... blobKeys) throws BlobstoreException;

}
