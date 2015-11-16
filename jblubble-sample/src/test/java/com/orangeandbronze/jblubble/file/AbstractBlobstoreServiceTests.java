package com.orangeandbronze.jblubble.file;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.input.CountingInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.orangeandbronze.jblubble.BlobInfo;
import com.orangeandbronze.jblubble.BlobKey;
import com.orangeandbronze.jblubble.BlobstoreException;
import com.orangeandbronze.jblubble.BlobstoreReadCallback;
import com.orangeandbronze.jblubble.BlobstoreService;

public abstract class AbstractBlobstoreServiceTests {

	protected BlobstoreService blobstoreService;
	protected BlobKey blobKey;

	protected abstract BlobstoreService createBlobstoreService();
	
	protected abstract long countBlobs();

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
				new File(AbstractBlobstoreServiceTests.class.getResource(
						inputFileName).toURI()).length(),
				outputFile.length());
		assertTrue(outputFile.delete());
	}

	@Test(expected = BlobstoreException.class)
	public void createAndDeleteBlob() throws Exception {
		BlobKey blobKey = createBlob("sample-image.png");
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
		assertNotNull(blobInfo.getDateCreated());
		assertEquals(blobKey, blobInfo.getBlobKey());
	}

	@Test
	public void createSeveralAndDeleteThem() throws Exception {
		BlobKey blobKeys[] = new BlobKey[4];
		for (int i = 0; i < blobKeys.length; i++) {
			blobKeys[i] = createBlob("sample-image.png");
		}
		blobstoreService.delete(blobKeys);
	}

	protected BlobKey createBlob(String inputFileName) throws BlobstoreException, IOException {
		BlobKey blobKey;
		InputStream in = AbstractBlobstoreServiceTests.class.getResourceAsStream(inputFileName);
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
		InputStream in = AbstractBlobstoreServiceTests.class.getResourceAsStream(inputFileName);
		assertNotNull("Input file not found [" + inputFileName + "]", in);
		try {
			blobKey = blobstoreService.createBlob((out) -> {
						byte[] buffer = new byte[1024];
						long totalLength = 0;
						int len;
						while ((len = in.read(buffer)) != -1) {
							out.write(buffer, 0, len);
							totalLength += len;
						}
						return totalLength;
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
				new File(AbstractBlobstoreServiceTests.class.getResource(
						inputFileName).toURI()).length(),
				outputFile.length());
		assertTrue(outputFile.delete());
	}

	@Test
	public void deleteAndCheckUpdateCounts() throws Exception {
		BlobKey blobKeys[] = new BlobKey[4];
		for (int i = 0; i < blobKeys.length - 1; i++) {
			blobKeys[i] = createBlob("sample-image.png");
		}
		blobKeys[3] = new BlobKey("1234"); // this ID does not exist
		int[] updateCounts = blobstoreService.delete(blobKeys);
		assertTrue(updateCounts[0] > 0);
		assertTrue(updateCounts[1] > 0);
		assertTrue(updateCounts[2] > 0);
		assertEquals(0, updateCounts[3]);
	}

	@Test
	public void noItemCreatedWhenInputStreamThrowsException() throws Exception {
		String inputFileName = "sample-image.png";
		InputStream in = AbstractBlobstoreServiceTests.class.getResourceAsStream(inputFileName);
		assertNotNull("Input file not found [" + inputFileName + "]", in);
		long originalCount = 0;
		try {
			in = new CountingInputStream(in) {
				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					if (getByteCount() > 4000) {
						throw new IOException("Intended exception for test");
					}
					return super.read(b, off, len);
				}

				@Override
				public int read(byte[] bts) throws IOException {
					if (getByteCount() > 4000) {
						throw new IOException("Intended exception for test");
					}
					return super.read(bts);
				}
			};
			originalCount = countBlobs();
			blobstoreService.createBlob(in, "test", "image/png");
			fail("Exception should have been thrown");
		} catch (BlobstoreException | IOException ioe) {
			assertEquals(originalCount, countBlobs());
		} finally {
			in.close();
		}
	}

	@Test
	public void createAndServeBlobByteRange() throws Exception {
		String inputFileName = "sample-image.png";
		blobKey = createBlob(inputFileName);
		File outputFile = new File("target/retrieved-image.png");
		OutputStream out = new FileOutputStream(outputFile);
		try {
			// sample-image.png is 6792 bytes long
			blobstoreService.serveBlob(blobKey, out, 0, 4000);
			blobstoreService.serveBlob(blobKey, out, 4001, 6791);
		} finally {
			out.close();
		}
		assertEquals(
				new File(AbstractBlobstoreServiceTests.class.getResource(
						inputFileName).toURI()).length(),
				outputFile.length());
		// Check if image is still intact
		BufferedImage outputImage = ImageIO.read(outputFile);
		assertEquals(252, outputImage.getWidth());
		assertEquals(150, outputImage.getHeight());
		ImageInputStream imageInputStream = ImageIO.createImageInputStream(outputFile);
		try {
			Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(imageInputStream);
			assertTrue(imageReaders.hasNext());
			assertEquals("png", imageReaders.next().getFormatName());
		} finally {
			imageInputStream.close();
		}
		assertTrue(outputFile.delete());
	}

	class ImageReadCallback implements BlobstoreReadCallback {

		private int width = -1;
		private int height = -1;
		private boolean called = false;

		@Override
		public void readInputStream(InputStream in) throws IOException {
			BufferedImage image = ImageIO.read(in);
			width = image.getWidth();
			height = image.getHeight();
			called = true;
		}
		
		public boolean isCalled() {
			return called;
		}

		public int getWidth() {
			if (! isCalled()) {
				throw new IllegalStateException();
			}
			return width;
		}

		public int getHeight() {
			if (! isCalled()) {
				throw new IllegalStateException();
			}
			return height;
		}

	}
	
	@Test
	public void readImageWidthAndHeight() throws Exception {
		blobKey = createBlob("sample-image.png");
		ImageReadCallback imageReaderCallback = new ImageReadCallback();
		blobstoreService.readBlob(blobKey, imageReaderCallback);
		assertTrue("Callback must be called", imageReaderCallback.isCalled());
		assertEquals(252, imageReaderCallback.getWidth());
		assertEquals(150, imageReaderCallback.getHeight());
	}

	@Test
	public void readBlobNotFound() throws Exception {
		blobKey = new BlobKey("1234"); // this ID does not exist
		try {
			blobstoreService.readBlob(blobKey, null);
			fail("Exception should have been thrown");
		} catch (BlobstoreException e) {
		}
	}

	@Test
	public void md5Hash() throws Exception {
		blobKey = createBlob("sample-image.png");
		BlobInfo blobInfo = blobstoreService.getBlobInfo(blobKey);
		assertEquals("1bc7471b09047a7e72481b38c8ee4da2", blobInfo.getMd5Hash());
	}

}