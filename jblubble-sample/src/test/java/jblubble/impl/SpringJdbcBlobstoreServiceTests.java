package jblubble.impl;

import javax.sql.DataSource;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jblubble.BlobstoreService;
import jblubble.impl.SpringJdbcBlobstoreService;

@ContextConfiguration("JdbcBlobstoreServiceTests-context.xml")
@RunWith(SpringJUnit4ClassRunner.class)
public class SpringJdbcBlobstoreServiceTests extends AbstractBlobstoreServiceTests {

	@Autowired
	private DataSource dataSource;

	@Override
	protected BlobstoreService createBlobstoreService() {
		return  new SpringJdbcBlobstoreService(dataSource);
	}

	@Override
	protected long countBlobs() {
		return 0;
	}

}
