package jblubble.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sql.DataSource;

import jblubble.BlobstoreException;
import jblubble.BlobstoreService;
import jblubble.BlobstoreWriteCallback;

public abstract class AbstractJdbcBlobstoreService implements BlobstoreService {

	public static final int DEFAULT_BUFFER_SIZE = 0x1000; // 4096 bytes, or 4 kilobytes
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
			"INSERT INTO %s (name, content_type, content, size, date_created) "
			+ "VALUES (?, ?, ?, ?, ?)";

	protected String getInsertSql() {
		return String.format(INSERT_SQL, getTableName());
	}
	
	private static final String SELECT_CONTENT_BY_ID_SQL =
			"SELECT content FROM %s WHERE id = ?";

	protected String getSelectContentByIdSql() {
		return String.format(SELECT_CONTENT_BY_ID_SQL, getTableName());
	}

	private static final String SELECT_NON_CONTENT_FIELDS_BY_ID_SQL =
			"SELECT name, content_type, size, date_created FROM %s WHERE id = ?";

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
	public String createBlob(InputStream in,
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

}