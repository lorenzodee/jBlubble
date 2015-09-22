package jblubble.impl;

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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import jblubble.BlobInfo;
import jblubble.BlobstoreException;
import jblubble.BlobstoreService;
import jblubble.BlobstoreWriteCallback;

/**
 * {@link BlobstoreService Blobstore service} implementation using
 * {@link JdbcTemplate} in Spring Framework.
 *
 */
public class SpringJdbcBlobstoreService extends AbstractJdbcBlobstoreService {

	private final JdbcTemplate jdbcTemplate;

	public SpringJdbcBlobstoreService(DataSource dataSource) {
		super(dataSource);
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public String createBlob(BlobstoreWriteCallback callback,
			String name, String contentType)
					throws IOException, BlobstoreException {
		try {
			GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(
					new PreparedStatementCreator() {
						@Override
						public PreparedStatement createPreparedStatement(
								Connection connection) throws SQLException {
							try {
								PreparedStatement ps = connection.prepareStatement(
										getInsertSql(),
										Statement.RETURN_GENERATED_KEYS);
								ps.setString(1, name);
								ps.setString(2, contentType);
								Blob content = connection.createBlob();
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
								return ps;
							} catch (IOException e) {
								throw new BlobstoreException(
										"Error when creating blob", e);
							}
						}
					}, keyHolder);
			return String.valueOf(keyHolder.getKey().longValue());
		} catch (DataAccessException e) {
			throw new BlobstoreException(e);
		}
	}

	@Override
	public BlobInfo getBlobInfo(String blobKey) throws BlobstoreException {
		try {
			return jdbcTemplate.query(
					getSelectNonContentFieldsByIdSql(),
					new ResultSetExtractor<BlobInfo>() {
						@Override
						public BlobInfo extractData(ResultSet rs)
								throws SQLException, DataAccessException {
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
					}, Long.valueOf(blobKey));
		} catch (DataAccessException e) {
			throw new BlobstoreException(e);
		}
	}

	@Override
	public void serveBlob(String blobKey, OutputStream out)
			throws IOException, BlobstoreException {
		try {
			jdbcTemplate.query(
					getSelectContentByIdSql(),
					new ResultSetExtractor<Long>() {
						@Override
						public Long extractData(ResultSet rs)
								throws SQLException, DataAccessException {
							if (!rs.next()) {
								throw new BlobstoreException(
										"Blob not found: " + blobKey);
							}
							Blob blob = rs.getBlob("content");
							try (InputStream in = blob.getBinaryStream()) {
								copy(in, out);
							} catch (IOException ioe) {
								throw new BlobstoreException(
										"Error while reading blob", ioe);
							}
							return blob.length();
						}
					}, Long.valueOf(blobKey));
		} catch (DataAccessException e) {
			throw new BlobstoreException(e);
		}
	}

	@Override
	public int[] delete(String... blobKeys) throws BlobstoreException {
		try {
			return jdbcTemplate.batchUpdate(
					getDeleteByIdSql(),
					new BatchPreparedStatementSetter() {
						@Override
						public void setValues(PreparedStatement ps, int i)
								throws SQLException {
							ps.setLong(1, Long.valueOf(blobKeys[i]));
						}
						
						@Override
						public int getBatchSize() {
							return blobKeys.length;
						}
					});
		} catch (DataAccessException e) {
			throw new BlobstoreException(e);
		}
	}

}
