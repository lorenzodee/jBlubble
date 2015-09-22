package jblubble.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jblubble.BlobInfo;
import jblubble.BlobstoreException;
import jblubble.BlobstoreService;
import jblubble.BlobstoreWriteCallback;

public abstract class AbstractBlobstoreServiceTests {

	protected BlobstoreService blobstoreService;
	protected String blobKey;

	protected abstract BlobstoreService createBlobstoreService();

	@Before
	public void setUp() throws Exception {
		blobstoreService = createBlobstoreService();
		assertNotNull(blobstoreService);
	}

	@After
	public void tearDown() throws Exception {
		if (blobKey != null) {
			blobstoreService.delete(blobKey);
		}
	}

	@Test
	public void createAndServeBlob() throws Exception {
		String inputFileName = "sample-image.png";
		blobKey = createBlob(inputFileName);
		File outputFile = new File("target/retrieved-image.png");
		OutputStream out = new FileOutputStream(outputFile);
		try {
			blobstoreService.serveBlob(blobKey, out);
		} finally {
			out.close();
		}
		assertEquals(
				new File(getClass().getResource(inputFileName).toURI()).length(),
				outputFile.length());
		assertTrue(outputFile.delete());
	}

	@Test(expected = BlobstoreException.class)
	public void createAndDeleteBlob() throws Exception {
		String blobKey = createBlob("sample-image.png");
		blobstoreService.delete(blobKey);
		assertNull(blobstoreService.getBlobInfo(blobKey));
		blobstoreService.serveBlob(blobKey, null);
	}

	@Test
	public void createAndGetInfo() throws Exception {
		blobKey = createBlob("sample-image.png");
		BlobInfo blobInfo = blobstoreService.getBlobInfo(blobKey);
		assertNotNull(blobInfo);
		assertEquals("test", blobInfo.getName());
		assertEquals("image/png", blobInfo.getContentType());
		assertEquals(6792, blobInfo.getSize());
		assertEquals(blobKey, blobInfo.getBlobKey());
	}

	@Test
	public void createSeveralAndDeleteThem() throws Exception {
		String blobKeys[] = new String[4];
		for (int i = 0; i < blobKeys.length; i++) {
			blobKeys[i] = createBlob("sample-image.png");
		}
		blobstoreService.delete(blobKeys);
	}

	protected String createBlob(String inputFileName) throws BlobstoreException, IOException {
		String blobKey;
		InputStream in = getClass().getResourceAsStream(inputFileName);
		assertNotNull("Input file not found [" + inputFileName + "]", in);
		try {
			blobKey = blobstoreService.createBlob(
					in, "test", "image/png");
			assertNotNull(blobKey);
		} finally {
			in.close();
		}
		return blobKey;
	}

	@Test
	public void createBlobWithOutputStream() throws Exception {
		String inputFileName = "sample-image.png";
		InputStream in = getClass().getResourceAsStream(inputFileName);
		assertNotNull("Input file not found [" + inputFileName + "]", in);
		try {
			blobKey = blobstoreService.createBlob(
					new BlobstoreWriteCallback() {
						@Override
						public long writeToOutputStream(OutputStream out) throws IOException {
							byte[] buffer = new byte[1024];
							long totalLength = 0;
							int len;
							while ((len = in.read(buffer)) != -1) {
								out.write(buffer, 0, len);
								totalLength += len;
							}
							return totalLength;
						}
					}, "test", "image/png");
			assertNotNull(blobKey);
		} finally {
			in.close();
		}
		File outputFile = new File("target/retrieved-image.png");
		OutputStream out = new FileOutputStream(outputFile);
		try {
			blobstoreService.serveBlob(blobKey, out);
		} finally {
			out.close();
		}
		assertEquals(
				new File(getClass().getResource(inputFileName).toURI()).length(),
				outputFile.length());
		assertTrue(outputFile.delete());
	}

	@Test
	public void deleteAndCheckUpdateCounts() throws Exception {
		String blobKeys[] = new String[4];
		for (int i = 0; i < blobKeys.length - 1; i++) {
			blobKeys[i] = createBlob("sample-image.png");
		}
		blobKeys[3] = "1234"; // this ID does not exist
		int[] updateCounts = blobstoreService.delete(blobKeys);
		assertEquals(1, updateCounts[0]);
		assertEquals(1, updateCounts[1]);
		assertEquals(1, updateCounts[2]);
		assertEquals(0, updateCounts[3]);
	}

}