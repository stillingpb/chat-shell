package chatshell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import chatshell.io.MessageWritable;

public class testDao {
	private SqlSessionFactory sqlMapper;

	@Before
	public void init() {
		String resource = "mybatis-config.xml";
		try {
			Reader reader = Resources.getResourceAsReader(resource);
			// InputStream in = new FileInputStream(resource);
			sqlMapper = new SqlSessionFactoryBuilder().build(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testAddMessage() {
		SqlSession session = sqlMapper.openSession();
		int result = 0;
		try {
			result = session.insert("chatshell.dao.MessageDao.addMessage", new MessageWritable(255,
					"pb", "love you"));
			session.commit();
		} finally {
			session.close();
		}
		assertEquals(result, 1);
	}

	@Test
	public void testFindLatestMessage() {
		SqlSession session = sqlMapper.openSession();
		List<MessageWritable> list = session.selectList(
				"chatshell.dao.MessageDao.findLatestMessageByTime", 235);
		assertNotEquals(list.size(), 0);
		System.out.println(list);
	}
}
