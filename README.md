# jBlubble

This project aims to make it easier to store and retrieve <abbr title="Binary Large OBject">BLOB</abbr>s (or binary large objects) to and from a database. Specifically, it aims to help with storing uploaded files and serving them in a web application. These files can be images (e.g. JPEG, PNG), PDF, or other potentially large objects (that would not fit well in a byte array or string).

The most common practice, albeit ill-advised, is to use byte arrays (`byte[]`). This often leads to out-of-memory failures as the files keep growing in size, and will not scale well. This project uses the lesser known features of JDBC 4.0 (Java 6) to store and retrieve BLOBs.

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
	...
}
```

...the BLOB is referenced.

```java
@Entity
public class Person {
	@Id private Long id;
	... BlobKey photoId; // retrieved via BlobstoreService
	...
}
```

## Motivation

You can read [blog post](http://lorenzo-dee.blogspot.com/2015/09/blob-handling-java-jdbc.html) to understand how this API began.
