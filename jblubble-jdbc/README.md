# jBlubble JDBC implementation

This contains a JDBC implementation of the `BlobstoreService` interface. It uses a table named `lobs` that contain the following columns:

- id (numeric primary key)
- name
- content type
- content (the BLOB)
- size (length of BLOB in bytes)
- date created (timestamp)

A sample <abbr title="Data Definition Language">DDL</abbr> file to create the `lobs` table is available at [src/main/resources/jblubble/impl](src/main/resources/jblubble/impl). The DDL is only tested for HSQL in-memory database. Ports of the DDL to other databases are welcome. 
