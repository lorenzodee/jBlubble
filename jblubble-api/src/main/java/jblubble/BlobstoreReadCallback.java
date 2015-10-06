package jblubble;

import java.io.IOException;
import java.io.InputStream;

/**
 * Call-back interface to read from an {@link InputStream input stream} that
 * will read the blob contents.
 *
 */
@FunctionalInterface
public interface BlobstoreReadCallback {

	/**
	 * Read blob contents.
	 * 
	 * @param in
	 *            the blob contents
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	void readInputStream(InputStream in) throws IOException;

}
