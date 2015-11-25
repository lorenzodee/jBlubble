# jBlubble JDBC implementation

This contains a JDBC implementation of the `BlobstoreService` interface. It uses a table named `lobs` that contain the following columns:

- id (numeric primary key)
- name
- content type
- content (the BLOB)
- size (length of BLOB in bytes)
- date created (timestamp)
- md5 hash (MD5 hash)

A sample <abbr title="Data Definition Language">DDL</abbr> file to create the `lobs` table is available at [src/main/resources/jblubble/jdbc](src/main/resources/jblubble/jdbc). The DDL is only tested for HSQL in-memory database. Ports of the DDL to other databases are welcome. 

With version 1.1, an implementation that uses the Spring Framework's `JdbcTemplate` is available. This has the added advantage of inherently being able to participate in Spring-managed transactions. Dependencies to the Spring Framework artifacts are declared as *optional* in the Maven POM.