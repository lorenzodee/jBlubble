package jblubble.sample;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import jblubble.BlobstoreService;
import jblubble.impl.JdbcBlobstoreService;

@Configuration
public class BlobstoreSampleAppConfig {
	
	@Bean
	public DataSource dataSource() {
		EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
		EmbeddedDatabase db = builder
				.setType(EmbeddedDatabaseType.HSQL)
				.addScript("classpath:/blobstore/sample/create-lob-table.sql")
				.build();
		return db;
	}

	@Bean
	public BlobstoreService blobstoreService() {
		return new JdbcBlobstoreService(dataSource());
	}

}
