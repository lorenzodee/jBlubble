package jblubble;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Call-back interface to write to an {@link OutputStream output stream} that
 * will set the blob contents.
 *
 */
public interface BlobstoreWriteCallback {

	/**
	 * Write to the output stream and optionally return the length of bytes
	 * written.
	 * 
	 * @param out
	 *            the output stream
	 * @return the number of bytes written, or <code>-1L</code> to let the
	 *         caller determine the number of bytes
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	long writeToOutputStream(OutputStream out) throws IOException;

}
