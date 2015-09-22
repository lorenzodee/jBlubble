package jblubble.impl;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jblubble.BlobstoreService;
import jblubble.impl.JdbcBlobstoreService;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcBlobstoreServiceTests extends AbstractBlobstoreServiceTests {

	@Autowired
	private DataSource dataSource;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private JdbcBlobstoreService blobstoreService;

	@Override
	protected BlobstoreService createBlobstoreService() {
		return (blobstoreService = new JdbcBlobstoreService(dataSource));
	}

	@Override
	protected long countBlobs() {
		return jdbcTemplate.queryForObject(
				"SELECT count(*) FROM " + blobstoreService.getTableName(),
				Long.class);
	}

}
