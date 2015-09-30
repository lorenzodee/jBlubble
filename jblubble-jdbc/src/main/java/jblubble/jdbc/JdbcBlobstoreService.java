package jblubble.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.io.output.CountingOutputStream;

import jblubble.BlobInfo;
import jblubble.BlobKey;
import jblubble.BlobstoreException;
import jblubble.BlobstoreService;
import jblubble.BlobstoreWriteCallback;

/**
 * {@link BlobstoreService Blobstore service} implementation using JDBC.
 * <p>
 * Requires Java 7 (due to try-with-resources) and JDBC 4.0 (due to
 * {@link Connection#createBlob()}).
 *
 */
public class JdbcBlobstoreService extends AbstractJdbcBlobstoreService {

	public JdbcBlobstoreService(DataSource dataSource) {
		super(dataSource);
	}

	protected long getGeneratedKey(PreparedStatement ps) throws SQLException {
		long generatedId;
		try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
			if (generatedKeys.next()) {
				generatedId = generatedKeys.getLong(1);
			} else {
				throw new BlobstoreException(
						"No unique key generated");
			}
		}
		return generatedId;
	}

	@Override
	public BlobKey createBlob(BlobstoreWriteCallback callback,
			String name, String contentType)
					throws IOException, BlobstoreException {
		try {
			try (
					Connection connection = dataSource.getConnection();
					PreparedStatement ps = connection.prepareStatement(
							getInsertSql(),
							Statement.RETURN_GENERATED_KEYS);
				) {
				ps.setString(1, name);
				ps.setString(2, contentType);
				Blob content = connection.createBlob();
				try {
					long size;
					OutputStream out = content.setBinaryStream(1L);
					try {
						CountingOutputStream countingOutputStream =
								new CountingOutputStream(out);
						try {
							size = callback.writeToOutputStream(
									countingOutputStream);
							if (size == -1L) {
								size = countingOutputStream.getByteCount();
							}
						} finally {
							countingOutputStream.close();
						}
					} finally {
						out.close();
					}
					ps.setBlob(3, content);
					ps.setLong(4, size);
					ps.setTimestamp(5, new java.sql.Timestamp(
							new java.util.Date().getTime()));
					int rowCount = ps.executeUpdate();
					if (rowCount == 0) {
						throw new BlobstoreException(
								"Creating blob failed, no rows created.");
					}
					long generatedId = getGeneratedKey(ps);
					return new BlobKey(String.valueOf(generatedId));
				} finally {
					content.free();
				}
			}
		} catch (SQLException e) {
			throw new BlobstoreException("Error when creating blob", e);
		}
	}

	@Override
	public BlobInfo getBlobInfo(BlobKey blobKey) throws BlobstoreException {
		try {
			try (
					Connection connection = dataSource.getConnection();
					PreparedStatement ps = connection.prepareStatement(
							getSelectNonContentFieldsByIdSql());
				) {
				ps.setLong(1, Long.valueOf(blobKey.stringValue()));
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						return null;
					}
					return new BlobInfo(
							blobKey,
							rs.getString("name"),
							rs.getString("content_type"),
							rs.getLong("size"),
							rs.getTimestamp("date_created"));
				}
			}
		} catch (SQLException e) {
			throw new BlobstoreException(
					"Error when getting blob info", e);
		}
	}

	@Override
	public void serveBlob(BlobKey blobKey, OutputStream out)
			throws IOException, BlobstoreException {
		serveBlob(blobKey, out, 1);
	}

	public void serveBlob(BlobKey blobKey, OutputStream out, long start)
			throws IOException, BlobstoreException {
		serveBlob(blobKey, out, start, -1);
	}

	public void serveBlob(BlobKey blobKey, OutputStream out, long start, long end)
			throws IOException, BlobstoreException {
		try {
			try (
					Connection connection = dataSource.getConnection();
					PreparedStatement ps = connection.prepareStatement(
							getSelectContentByIdSql());
				) {
				ps.setLong(1, Long.valueOf(blobKey.stringValue()));
				try (ResultSet rs = ps.executeQuery()) {
					if (!rs.next()) {
						throw new BlobstoreException(
								"Blob not found: " + blobKey);
					}
					Blob blob = rs.getBlob(1);
					try {
						try (InputStream in = blob.getBinaryStream()) {
							copy(in, out);
						}
					} finally {
						blob.free();
					}
				}
			}
		} catch (SQLException e) {
			throw new BlobstoreException("Error when retrieving blob", e);
		}
	}

	@Override
	public int[] delete(BlobKey... blobKeys) throws BlobstoreException {
		for (BlobKey blobKey : blobKeys) {
			if (blobKey == null) {
				throw new IllegalArgumentException(
						"Blob keys cannot be null");
			}
		}
		try {
			try (
					Connection connection = dataSource.getConnection();
					PreparedStatement ps = connection.prepareStatement(
							getDeleteByIdSql());
				) {
				for (BlobKey blobKey : blobKeys) {
					ps.setLong(1, Long.valueOf(blobKey.stringValue()));
					ps.addBatch();
				}
				return ps.executeBatch();
			}
		} catch (SQLException e) {
			throw new BlobstoreException(
					"Error when deleting blobs", e);
		}
	}

}
