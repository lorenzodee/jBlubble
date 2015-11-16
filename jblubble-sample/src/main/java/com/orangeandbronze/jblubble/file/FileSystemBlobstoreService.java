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
package com.orangeandbronze.jblubble.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import com.orangeandbronze.jblubble.BlobInfo;
import com.orangeandbronze.jblubble.BlobKey;
import com.orangeandbronze.jblubble.BlobstoreException;
import com.orangeandbronze.jblubble.BlobstoreReadCallback;
import com.orangeandbronze.jblubble.BlobstoreService;
import com.orangeandbronze.jblubble.BlobstoreWriteCallback;

/**
 * {@link BlobstoreService Blobstore service} implementation using the file
 * system.
 * <p>
 * BLOB contents are stored in one file, and meta data is stored in another
 * file. Both files have the same name, but have different extensions.
 * </p>
 *
 * @author Lorenzo Dee
 */
public class FileSystemBlobstoreService implements BlobstoreService {

	public static final int DEFAULT_BUFFER_SIZE = 0x1000; // 4096 bytes, or 4 kilobytes

	private final File rootDirectory;
	private int bufferSize = DEFAULT_BUFFER_SIZE;

	public FileSystemBlobstoreService(File rootDirectory) {
		if (rootDirectory == null || !rootDirectory.isDirectory()) {
			throw new IllegalArgumentException(
					"Root directory cannot be null, and must be a directory");
		}
		this.rootDirectory = rootDirectory;
	}
	
	public File getRootDirectory() {
		return rootDirectory;
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

	protected void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[getBufferSize()];
		int len;
		while ((len = in.read(buffer)) != -1) {
			out.write(buffer, 0, len);
		}
	}

	protected void copy(InputStream in, OutputStream out, long length) throws IOException {
		byte[] buffer = new byte[getBufferSize()];
		int len;
		long total = 0L;
		while ((len = in.read(buffer)) != -1) {
			total += len;
			if (total > length) {
				len = (int) (length);
			}
			out.write(buffer, 0, len);
			if (total > length) {
				break;
			}
		}
	}

	@Override
	public BlobKey createBlob(InputStream in, String name, String contentType)
			throws IOException, BlobstoreException {
		return createBlob((BlobstoreWriteCallback) (out) -> {
			copy(in, out);
			return -1L;
		}, name, contentType);
	}

	@Override
	public BlobKey createBlob(BlobstoreWriteCallback callback, String name, String contentType)
			throws IOException, BlobstoreException {
		String uniqueId = generateUniqueId();
		File contentFile = createContentFile(uniqueId);
		String md5Hash = null;
		long size;
		try {
			try (FileOutputStream out = new FileOutputStream(contentFile)) {
				MessageDigest md5;
				try {
					md5 = MessageDigest.getInstance(MD5_ALGORITHM_NAME);
					try (DigestOutputStream digestOutputStream =
							new DigestOutputStream(out, md5)) {
						size = callback.writeToOutputStream(
								digestOutputStream);
						if (size == -1L) {
							out.flush();
							size = contentFile.length();
						}
						md5Hash = new String(encodeHex(md5.digest()));
					}
				} catch (NoSuchAlgorithmException e) {
					throw new BlobstoreException(e);
				}
				callback.writeToOutputStream(out);
			}
		} catch (IOException e) {
			contentFile.delete();
			throw e;
		}
		File metaFile = createMetaFile(uniqueId);
		try {
			// TODO Refactor to method to write/read meta file
			// BlobInfo blobInfo = new BlobInfo(blobKey, name, contentType, size, dateCreated, md5Hash);
			// writeMetaFile(uniqueId, blobInfo);
			try (FileOutputStream meta = new FileOutputStream(metaFile)) {
				Properties props = new Properties();
				props.put("name", name);
				props.put("contentType", contentType);
				props.put("size", String.valueOf(size));
				props.put("dateCreated", String.valueOf(new Date().getTime()));
				props.put("md5Hash", md5Hash);
				props.store(meta, null);
			}
			return new BlobKey(uniqueId);
		} catch (IOException e) {
			metaFile.delete();
			throw e;
		}
	}

	protected String generateUniqueId() {
		return UUID.randomUUID().toString();
	}

	protected File getContentFile(BlobKey blobKey) {
		return createContentFile(blobKey.stringValue());
	}

	protected File createContentFile(String uniqueId) {
		return new File(getRootDirectory(), uniqueId + ".dat");
	}

	protected File getMetaFile(BlobKey blobKey) {
		return createMetaFile(blobKey.stringValue());
	}

	protected File createMetaFile(String uniqueId) {
		return new File(getRootDirectory(), uniqueId + ".properties");
	}

	protected String toFileName(String uniqueId) {
		return uniqueId + ".dat";
	}

	@Override
	public BlobInfo getBlobInfo(BlobKey blobKey) throws BlobstoreException {
		try {
			File metaFile = getMetaFile(blobKey);
			// TODO Refactor to method to write/read meta file
			// BlobInfo blobInfo = readMetaFile(uniqueId);
			try (FileInputStream meta = new FileInputStream(metaFile)) {
				Properties props = new Properties();
				props.load(meta);
				String name = props.getProperty("name");
				String contentType = props.getProperty("contentType");
				long size = Long.valueOf(props.getProperty("size"));
				Date dateCreated = new Date(Long.valueOf(props.getProperty("dateCreated")));
				String md5Hash = props.getProperty("md5Hash");
				return new BlobInfo(
						blobKey, name, contentType, size, dateCreated, md5Hash);
			}
		} catch (FileNotFoundException e) {
			return null;
		} catch (Exception e) {
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
			BlobKey blobKey, OutputStream out, long start, long end, boolean useEnd)
			throws IOException, BlobstoreException {
		try {
			File contentFile = getContentFile(blobKey);
			try (FileInputStream in = new FileInputStream(contentFile)) {
				if (start > 0) {
					in.skip(start - 1);
				}
				if (useEnd) {
					copy(in, out, end);
				} else {
					copy(in, out);
				}
			}
		} catch (FileNotFoundException e) {
			throw new BlobstoreException(e);
		}
	}

	@Override
	public int[] delete(BlobKey... blobKeys) throws BlobstoreException {
		int[] updateCounts = new int[blobKeys.length];
		for (int i = 0; i < blobKeys.length; i++) {
			updateCounts[i] = 0;
			updateCounts[i] += getContentFile(blobKeys[i]).delete() ? 1 : 0;
			updateCounts[i] += getMetaFile(blobKeys[i]).delete() ? 1 : 0;
		}
		return updateCounts;
	}

	@Override
	public void readBlob(BlobKey blobKey, BlobstoreReadCallback callback)
			throws IOException, BlobstoreException {
		try {
			File contentFile = getContentFile(blobKey);
			try (FileInputStream in = new FileInputStream(contentFile)) {
				callback.readInputStream(in);
			}
		} catch (FileNotFoundException e) {
			throw new BlobstoreException(e);
		}
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
