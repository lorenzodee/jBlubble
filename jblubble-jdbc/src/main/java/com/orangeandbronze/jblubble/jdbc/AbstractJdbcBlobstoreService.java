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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sql.DataSource;

import com.orangeandbronze.jblubble.BlobKey;
import com.orangeandbronze.jblubble.BlobstoreException;
import com.orangeandbronze.jblubble.BlobstoreService;
import com.orangeandbronze.jblubble.BlobstoreWriteCallback;

public abstract class AbstractJdbcBlobstoreService implements BlobstoreService {

	public static final int DEFAULT_BUFFER_SIZE = 0x2000; // 8192 bytes, or 8 kilobytes
	public static final String DEFAULT_TABLE_NAME = "lobs";

	protected final DataSource dataSource;
	private String tableName = DEFAULT_TABLE_NAME;
	private int bufferSize = DEFAULT_BUFFER_SIZE;

	public AbstractJdbcBlobstoreService(DataSource dataSource) {
		if (dataSource == null) {
			throw new IllegalArgumentException(
					"Datasource cannot be null");
		}
		this.dataSource = dataSource;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		if (bufferSize <= 0) {
			throw new IllegalArgumentException(
					"Buffer size cannot be zero or less");
		}
		this.bufferSize = bufferSize;
	}

	private static final String INSERT_SQL =
			"INSERT INTO %s (name, content_type, content, size, date_created, md5_hash) "
			+ "VALUES (?, ?, ?, ?, ?, ?)";

	protected String getInsertSql() {
		return String.format(INSERT_SQL, getTableName());
	}
	
	private static final String SELECT_CONTENT_BY_ID_SQL =
			"SELECT content FROM %s WHERE id = ?";

	protected String getSelectContentByIdSql() {
		return String.format(SELECT_CONTENT_BY_ID_SQL, getTableName());
	}

	private static final String SELECT_NON_CONTENT_FIELDS_BY_ID_SQL =
			"SELECT name, content_type, size, date_created, md5_hash FROM %s WHERE id = ?";

	protected String getSelectNonContentFieldsByIdSql() {
		return String.format(SELECT_NON_CONTENT_FIELDS_BY_ID_SQL, getTableName());
	}

	private static final String DELETE_BY_ID_SQL =
			"DELETE FROM lobs WHERE id = ?";

	protected String getDeleteByIdSql() {
		return String.format(DELETE_BY_ID_SQL, getTableName());
	}

	protected void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[getBufferSize()];
		int len;
		while ((len = in.read(buffer)) != -1) {
			out.write(buffer, 0, len);
		}
	}

	@Override
	public BlobKey createBlob(InputStream in,
			String name, String contentType)
			throws IOException, BlobstoreException {
		return createBlob(new BlobstoreWriteCallback() {
			@Override
			public long writeToOutputStream(OutputStream out)
					throws IOException {
				copy(in, out);
				return -1L;
			}
		}, name, contentType);
	}

	protected static final String MD5_ALGORITHM_NAME = "MD5";

	protected static final char[] HEX_CHARS =
			{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	protected static char[] encodeHex(byte[] bytes) {
		char chars[] = new char[32];
		for (int i = 0; i < chars.length; i = i + 2) {
			byte b = bytes[i / 2];
			chars[i] = HEX_CHARS[(b >>> 0x4) & 0xf];
			chars[i + 1] = HEX_CHARS[b & 0xf];
		}
		return chars;
	}

}