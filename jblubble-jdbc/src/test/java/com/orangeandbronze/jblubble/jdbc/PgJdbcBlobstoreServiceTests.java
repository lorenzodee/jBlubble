package com.orangeandbronze.jblubble.jdbc;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import com.orangeandbronze.jblubble.BlobstoreService;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PgJdbcBlobstoreServiceTests extends AbstractBlobstoreServiceTests {

	@Autowired
	private DataSource dataSource;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private JdbcBlobstoreService blobstoreService;

	@Override
	protected BlobstoreService createBlobstoreService() {
		return (blobstoreService = new PgJdbcBlobstoreService(dataSource));
	}

	@Override
	protected long countBlobs() {
		return jdbcTemplate.queryForObject(
				"SELECT count(*) FROM " + blobstoreService.getTableName(),
				Long.class);
	}

	@Override
	protected PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}
	
}
