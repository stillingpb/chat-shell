package chatday.dao;

import java.util.List;

import chatday.io.MessageWritable;

/**
 * 
 * @author pb
 * 
 */
public interface MessageDao {
	public boolean addMessage(MessageWritable msg);

	public List<MessageWritable> findLatestMessageByTime(long beginTime);
}
