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
package com.orangeandbronze.jblubble.jdbc.springframework;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.io.output.CountingOutputStream;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import com.orangeandbronze.jblubble.BlobInfo;
import com.orangeandbronze.jblubble.BlobKey;
import com.orangeandbronze.jblubble.BlobstoreException;
import com.orangeandbronze.jblubble.BlobstoreReadCallback;
import com.orangeandbronze.jblubble.BlobstoreService;
import com.orangeandbronze.jblubble.BlobstoreWriteCallback;
import com.orangeandbronze.jblubble.jdbc.AbstractJdbcBlobstoreService;

/**
 * A {@link BlobstoreService blobstore service} implementation using
 * {@link JdbcTemplate} of the Spring Framework. Using {@link JdbcTemplate JDBC
 * template} translates JDBC exceptions to {@link DataAccessException}s.
 * <p>
 * This has the added advantage of inherently being able to participate in
 * Spring-managed transactions.
 * </p>
 * 
 * @author Lorenzo Dee
 */
public class SpringJdbcBlobstoreService extends AbstractJdbcBlobstoreService {

	private final JdbcTemplate jdbcTemplate;

	public SpringJdbcBlobstoreService(DataSource dataSource) {
		super(dataSource);
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public BlobKey createBlob(BlobstoreWriteCallback callback,
			String name, String contentType)
					throws IOException, BlobstoreException {
		try {
			GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
			jdbcTemplate.update(
					(connection) -> {
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
					}, keyHolder);
			return new BlobKey(
					String.valueOf(keyHolder.getKey().longValue()));
		} catch (DataAccessException e) {
			throw new BlobstoreException(e);
		}
	}

	@Override
	public BlobInfo getBlobInfo(BlobKey blobKey) throws BlobstoreException {
		try {
			return jdbcTemplate.query(
					getSelectNonContentFieldsByIdSql(),
					(rs) -> {
						if (!rs.next()) {
							return null;
						}
						return new BlobInfo(
								blobKey,
								rs.getString("name"),
								rs.getString("content_type"),
								rs.getLong("size"),
								rs.getTimestamp("date_created"));
					}, Long.valueOf(blobKey.stringValue()));
		} catch (DataAccessException e) {
			throw new BlobstoreException(e);
		}
	}

	@Override
	public void serveBlob(BlobKey blobKey, OutputStream out)
			throws IOException, BlobstoreException {
		serveBlob(blobKey, out, 0);
	}

	@Override
	public void serveBlob(BlobKey blobKey, OutputStream out, long start)
			throws IOException, BlobstoreException {
		serveBlobInternal(blobKey, out, start, -1, false);
	}

	@Override
	public void serveBlob(BlobKey blobKey, OutputStream out, long start, long end)
			throws IOException, BlobstoreException {
		serveBlobInternal(blobKey, out, start, end, true);
	}

	protected void serveBlobInternal(
			BlobKey blobKey, OutputStream out, long start, long end, boolean useEnd) {
		try {
			jdbcTemplate.query(
					getSelectContentByIdSql(),
					(rs) -> {
						if (!rs.next()) {
							throw new BlobstoreException(
									"Blob not found: " + blobKey);
						}
						Blob blob = rs.getBlob("content");
						try {
							long pos = start + 1;
							long length = useEnd ? (end - start + 1) : blob.length();
							try (InputStream in = blob.getBinaryStream(pos, length)) {
								copy(in, out);
							} catch (IOException ioe) {
								throw new BlobstoreException(
										"Error while reading blob", ioe);
							}
							return blob.length();
						} finally {
							blob.free();
						}
					}, Long.valueOf(blobKey.stringValue()));
		} catch (DataAccessException e) {
			throw new BlobstoreException(e);
		}
	}

	@Override
	public int[] delete(BlobKey... blobKeys) throws BlobstoreException {
		try {
			return jdbcTemplate.batchUpdate(
					getDeleteByIdSql(),
					new BatchPreparedStatementSetter() {
						@Override
						public void setValues(PreparedStatement ps, int i)
								throws SQLException {
							ps.setLong(1, Long.valueOf(blobKeys[i].stringValue()));
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

	@Override
	public void readBlob(BlobKey blobKey, BlobstoreReadCallback callback)
			throws IOException, BlobstoreException {
		try {
			jdbcTemplate.query(
					getSelectContentByIdSql(),
					(rs) -> {
						if (!rs.next()) {
							throw new BlobstoreException(
									"Blob not found: " + blobKey);
						}
						Blob blob = rs.getBlob("content");
						try {
							try (InputStream in = blob.getBinaryStream()) {
								callback.readInputStream(in);
								return true;
							} catch (IOException ioe) {
								throw new BlobstoreException(
										"Error while reading blob", ioe);
							}
						} finally {
							blob.free();
						}
					}, Long.valueOf(blobKey.stringValue()));
		} catch (DataAccessException e) {
			throw new BlobstoreException(e);
		}
	}

}
