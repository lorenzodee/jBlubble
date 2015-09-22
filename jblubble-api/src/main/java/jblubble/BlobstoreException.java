package jblubble;

@SuppressWarnings("serial")
public class BlobstoreException extends RuntimeException {

	public BlobstoreException() {
		super();
	}

	public BlobstoreException(String message, Throwable cause) {
		super(message, cause);
	}

	public BlobstoreException(String message) {
		super(message);
	}

	public BlobstoreException(Throwable cause) {
		super(cause);
	}

}
