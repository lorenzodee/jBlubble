# jBlubble

This project was originally born from storing uploaded files and serving them in a web application. These files are typically images (e.g. JPEG, PNG), PDFs, or other potentially large objects (that would not fit well in a byte array or string).

After a few months, this project has evolved to making it easier to store and retrieve <abbr title="Binary Large OBject">BLOB</abbr>s (or binary large objects) to and from a persistent storage (e.g. relational database). A strategy interface is provided to abstract away the storage mechanism, and allowing different storage-specific implementations (e.g. database, file system, Amazon S3, Google Cloud Storage).

The most common practice of storing BLOBs, albeit ill-advised, is to use byte arrays (`byte[]`). This often leads to out-of-memory failures as the files keep growing in size, and will not scale well. This project uses the lesser known features of JDBC 4.0 (Java 6) to store and retrieve BLOBs to and from relational databases.

## Getting Started

When handling file uploads in servlets (3.0 and above), this is how it can be used:

```java
// Servlet configured to support multipart/form-data
// HttpServletRequest
request.getPart("...").write(fileName);
// Open an input stream with the file (created from uploaded part)
InputStream in = new FileInputStream(fileName);
try {
	... blobKey = blobstoreService.createBlob(in, ...);
} finally {
	in.close();
}
```

```java
// HttpServletResponse
BlobInfo blobInfo = blobstoreService.getBlobInfo(blobKey);
response.setContentType(blobInfo.getContentType());
OutputStream out = response.getOutputStream();
blobstoreService.serveBlob(blobKey, out);
```

When using Spring MVC, this is how it can be used inside a controller:

```java
// MultipartFile
... blobKey = blobstoreService.createBlob(
    multipartFile.getInputStream(), multipartFile.getName(), ...);
```

## Maven Dependency

To use in your Maven build, add the API and implementation(s) to your `pom.xml`.

	<dependency>
		<groupId>com.orangeandbronze</groupId>
		<artifactId>jblubble-api</artifactId>
		<version>1.1</version>
	</dependency>
	<dependency>
		<groupId>com.orangeandbronze</groupId>
		<artifactId>jblubble-jdbc</artifactId>
		<!-- jblubble-filesystem or some other implementation -->
		<version>1.1</version>
	</dependency>


## Running the Sample

A sample webapp to help get you started is in [jblubble-sample](jblubble-sample). To run the sample, install dependencies first, then run the web app:

	> cd jblubble-api
	> mvn install
	> cd ../jblubble-jdbc
	> mvn install
	> cd ../jblubble-sample
	> mvn tomcat7:run

Then go to [http://localhost:8080/jblubble-sample/uploads](http://localhost:8080/jblubble-sample/uploads).

## Using BLOBs

When using <abbr title="Binary Large OBject">BLOB</abbr>s in persistence objects, it is *recommended* that the object should not embed a BLOB. But instead, reference to one. For example, let's say we have a `Person` object that stores a photo image. Instead of having the photo embedded in the `Person` object, it can have the blob key to reference the photo blob.

So, instead of this...

```java
@Entity
public class Person {
	@Id private Long id;
	@Lob private byte[] photo;
	private int photoWidth, photoHeight;
	...
}
```

...the BLOB is referenced.

```java
@Entity
public class Person {
	@Id private Long id;
	private Image photo;
	...
}
```

```java
@Embeddable
public class Image {
	private BlobKey imageId; // retrieved via BlobstoreService
	private int photoWidth, photoHeight;
	...
}
```

The above also has the benefit of providing the width and height of the image *before* it is streamed to the client (in this case, usually a browser).

Using this pattern of referencing the BLOB (instead of modifying it with application-specific attributes) will keep BLOB handling simple and more re-usable.

## Motivation

You can read [blog post](http://lorenzo-dee.blogspot.com/2015/09/blob-handling-java-jdbc.html) to understand how this API began.
