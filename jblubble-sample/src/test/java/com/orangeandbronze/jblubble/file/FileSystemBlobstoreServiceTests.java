package com.orangeandbronze.jblubble.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import com.orangeandbronze.jblubble.BlobstoreService;

public class FileSystemBlobstoreServiceTests extends AbstractBlobstoreServiceTests {

	private FileSystemBlobstoreService blobstoreService;

	@Override
	protected BlobstoreService createBlobstoreService() {
		File rootDirectory = new File("./target");
		return (blobstoreService = new FileSystemBlobstoreService(rootDirectory));
	}

	@Override
	protected long countBlobs() {
		try {
			return Files.list(
					blobstoreService.getRootDirectory().toPath())
					.filter((f) -> { return f.toFile().getName().endsWith(".dat"); })
					.count();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
