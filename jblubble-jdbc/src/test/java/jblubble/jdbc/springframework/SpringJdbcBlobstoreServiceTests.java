package jblubble.jdbc.springframework;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

import jblubble.BlobstoreService;
import jblubble.jdbc.AbstractBlobstoreServiceTests;
import jblubble.jdbc.springframework.SpringJdbcBlobstoreService;

@ContextConfiguration("SpringJdbcBlobstoreServiceTests-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringJdbcBlobstoreServiceTests extends AbstractBlobstoreServiceTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private SpringJdbcBlobstoreService springJdbcBlobstoreService;

	@Override
	protected BlobstoreService createBlobstoreService() {
		return springJdbcBlobstoreService;
	}

	@Override
	protected long countBlobs() {
		return jdbcTemplate.queryForObject(
				"SELECT count(*) FROM " + springJdbcBlobstoreService.getTableName(),
				Long.class);
	}

	@Override
	protected PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

}
