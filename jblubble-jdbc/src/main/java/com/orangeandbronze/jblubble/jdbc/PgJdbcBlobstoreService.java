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
package com.orangeandbronze.jblubble.jdbc;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.io.output.CountingOutputStream;

import com.orangeandbronze.jblubble.BlobKey;
import com.orangeandbronze.jblubble.BlobstoreException;
import com.orangeandbronze.jblubble.BlobstoreWriteCallback;
import com.orangeandbronze.jblubble.jdbc.JdbcBlobstoreService;

/**
 * <p>
 * The standard way to create a {@link Blob BLOB} in JDBC is to call
 * {@link Connection#createBlob() connection.createBlob()}. However, this does
 * not work for PostgreSQL (as of version 9.4-1202-jdbc42). This implementation
 * uses a work-around to call a PostgreSQL function to create the BLOB, then use
 * JDBC to update it.
 * </p>
 * <p>
 * In PostgreSQL, <a href=
 * "https://jdbc.postgresql.org/documentation/publicapi/org/postgresql/largeobject/LargeObject.html">
 * <code>LargeObject</code></a>s are used for BLOBs. <code>LargeObject</code>s
 * may not be used in auto-commit mode. So, when reading/writing BLOBs, this
 * implementation sets auto-commit to <code>false</code> if it hasn't been set.
 * If changed, the auto-commit mode is reset back to its original value before
 * methods in this implementation was called.
 * </p>
 *
 * @author Lorenzo Dee
 * @author Marcial "Mike" Pua, Jr.
 *
 */
public class PgJdbcBlobstoreService extends JdbcBlobstoreService {

	public PgJdbcBlobstoreService(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public BlobKey createBlob(BlobstoreWriteCallback callback, String name, String contentType)
			throws IOException, BlobstoreException {
		boolean resetCommitMode = false;
		try (Connection connection = dataSource.getConnection()) {
			if (connection.getAutoCommit()) {
				connection.setAutoCommit(false);
				resetCommitMode = true;
			}
			try {
				int rowCount;
				try (PreparedStatement ps = connection.prepareStatement(
						getInsertSql(),
						Statement.RETURN_GENERATED_KEYS)) {
					ps.setString(1, name);
					ps.setString(2, contentType);
					ps.setTimestamp(3, new java.sql.Timestamp(
							new java.util.Date().getTime()));
					rowCount = ps.executeUpdate();
					if (rowCount == 0) {
						throw new BlobstoreException(
								"Creating blob failed, no rows created.");
					}
					final long generatedId = getGeneratedKey(ps);
					long size;
					String md5Hash = null;
					try (PreparedStatement ps2 = connection.prepareStatement(
							getSelectContentByIdSql())) {
						ps2.setLong(1, generatedId);
						ResultSet rs = ps2.executeQuery();
						if (!rs.next()) {
							throw new BlobstoreException(
									"Creating blob failed, no rows created.");
						}
						Blob contentBlob = rs.getBlob(1);
						try {
							OutputStream out = new BufferedOutputStream(
									contentBlob.setBinaryStream(1L), getBufferSize());
							try {
								CountingOutputStream countingOutputStream =
										new CountingOutputStream(out);
								try {
									MessageDigest md5;
									try {
										md5 = MessageDigest.getInstance(MD5_ALGORITHM_NAME);
										try (DigestOutputStream digestOutputStream =
												new DigestOutputStream(countingOutputStream, md5)) {
											size = callback.writeToOutputStream(
													digestOutputStream);
											if (size == -1L) {
												size = countingOutputStream.getByteCount();
											}
											md5Hash = new String(encodeHex(md5.digest()));
										}
									} catch (NoSuchAlgorithmException e) {
										throw new BlobstoreException(e);
									}
								} finally {
									try {
										countingOutputStream.close();
									} catch (IOException e) {
										// Since digestOutputStream gets closed,
										// the wrapped countingOutputStream does
										// not really need to get closed again.
									}
								}
							} finally {
								try {
									out.close();
								} catch (IOException e) {
									// Since digestOutputStream gets closed,
									// the wrapped buffered OutputStream does
									// not really need to get closed again.
								}
							}
						} finally {
							contentBlob.free();
						}
					}
					try (PreparedStatement ps3 = connection.prepareStatement(
							getUpdateSizeSql())) {
						ps3.setLong(1, size);
						ps3.setString(2, md5Hash);
						ps3.setLong(3, generatedId);
						rowCount = ps3.executeUpdate();
						if (rowCount == 0) {
							throw new BlobstoreException(
									"Creating blob failed, no rows created.");
						}
					}
					if (resetCommitMode) {
						connection.commit();
					}
					return new BlobKey(String.valueOf(generatedId));
				}
			} catch (Exception e) {
				connection.rollback();
				throw e;
			} finally {
				if (resetCommitMode) {
					connection.setAutoCommit(true);
				}
			}
		} catch (SQLException e) {
			throw new BlobstoreException("Error when creating blob", e);
		}
	}

	// Creates an empty blob
	private static final String INSERT_EMPTY_BLOB_SQL =
			"INSERT INTO %s (name, content_type, content, size, date_created, md5_hash) "
			+ "VALUES (?, ?, lo_creat(-1), -1, ?, null)";

	@Override
	protected String getInsertSql() {
		return String.format(INSERT_EMPTY_BLOB_SQL, getTableName());
	}
	
	// Updates size and MD5 hash values
	private static final String UPDATE_SIZE_AND_MD5_HASH_SQL =
			"UPDATE %s SET size = ?, md5_hash = ? WHERE id = ?";

	protected String getUpdateSizeSql() {
		return String.format(UPDATE_SIZE_AND_MD5_HASH_SQL, getTableName());
	}

	@Override
	protected void readBlobInternal(BlobKey blobKey, BlobHandler blobHandler)
			throws IOException, BlobstoreException {
		try {
			boolean resetCommitMode = false;
			try (Connection connection = dataSource.getConnection()) {
				if (connection.getAutoCommit()) {
					connection.setAutoCommit(false);
					resetCommitMode = true;
				}
				try (PreparedStatement ps = connection.prepareStatement(
								getSelectContentByIdSql())) {
					ps.setLong(1, Long.valueOf(blobKey.stringValue()));
					try (ResultSet rs = ps.executeQuery()) {
						if (!rs.next()) {
							throw new BlobstoreException(
									"Blob not found: " + blobKey);
						}
						Blob blob = rs.getBlob(1);
						try {
							blobHandler.handleBlob(blob);
						} finally {
							blob.free();
						}
					}
					if (resetCommitMode) {
						connection.commit();
					}
				}
			}
		} catch (SQLException e) {
			throw new BlobstoreException("Error when retrieving blob", e);
		}
	}
	
}
