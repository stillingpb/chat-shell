package chatday;

import ioc.PraticalBeanFactory;
import ioc.annotation.Component;
import ioc.util.BeanLoaderException;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import rpc.ipc.RPC;
import rpc.ipc.Server;
import chatday.dao.MessageDao;
import chatday.io.MessageWritable;
import chatday.io.MessageWritableList;
import chatday.rpcserver.RPCServerProtocol;

@Component
public class ChatServer implements RPCServerProtocol {

	@Inject
	@Named("messageDaoImpl")
	private MessageDao messageDao;

	@Override
	public boolean clientRegister() {
		return false;
	}

	@Override
	public MessageWritableList getHistoryMessage(long beginTime) {
		List<MessageWritable> list = messageDao.findLatestMessageByTime(beginTime);
		MessageWritableList msgList = new MessageWritableList(list);
		return msgList;
	}

	public static void main(String[] args) throws BeanLoaderException {
		String pck[] = { "chatday" };
		PraticalBeanFactory beanFactory = new PraticalBeanFactory(pck);
		ChatServer chatServer = beanFactory.getBean(ChatServer.class);
		Server rpcServer = RPC.getServer(chatServer, Chat.SERVER_HOST, Chat.SERVER_RPC_PORT);
		rpcServer.start();
	}
}
