DROP TABLE IF EXISTS lobs;

CREATE TABLE lobs
(
  id bigserial NOT NULL,
  name VARCHAR(255),
  content_type VARCHAR(255),
  content OID,
  "size" bigint NOT NULL,
  date_created timestamp without time zone NOT NULL,
  md5_hash VARCHAR(255),
  PRIMARY KEY (id)
)
WITH OIDS;

CREATE OR REPLACE RULE removelobcontent AS ON DELETE TO lobs
  DO SELECT lo_unlink( OLD.content );
