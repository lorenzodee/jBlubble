DROP TABLE lobs IF EXISTS;

CREATE TABLE lobs (
	id bigint generated by default as identity (start with 1),
	name varchar(255),
	content_type varchar(255),
	content BLOB,
	size bigint,
	date_created TIMESTAMP, 
	primary key (id)
);