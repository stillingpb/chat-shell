package chatshell.dao;

import ioc.annotation.Component;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import javax.inject.Named;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import chatshell.io.MessageWritable;

@Component
@Named("messageDaoImpl")
public class MessageDaoImpl implements MessageDao {
	private SqlSessionFactory sqlMapper;

	public MessageDaoImpl() {
		String resource = "mybatis-config.xml";
		try {
			Reader reader = Resources.getResourceAsReader(resource);
			sqlMapper = new SqlSessionFactoryBuilder().build(reader);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean addMessage(MessageWritable msg) {
		SqlSession session = sqlMapper.openSession();
		int result = 0;
		try {
			result = session.insert("chatshell.dao.MessageDao.addMessage", new MessageWritable(255,
					"pb", "love you"));
			session.commit();
			return result == 1;
		} finally {
			session.close();
		}
	}

	@Override
	public List<MessageWritable> findLatestMessageByTime(long beginTime) {
		SqlSession session = sqlMapper.openSession();
		try {
			List<MessageWritable> list = session.selectList(
					"chatshell.dao.MessageDao.findLatestMessageByTime", beginTime);
			return list;
		} finally {
			session.close();
		}
	}

}
