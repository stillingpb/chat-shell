package chatshell;

import ioc.PraticalBeanFactory;
import ioc.annotation.Component;
import ioc.util.BeanLoaderException;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import rpc.io.LongWritable;
import rpc.ipc.RPC;
import rpc.ipc.server.Server;
import chatshell.dao.MessageDao;
import chatshell.io.MessageWritable;
import chatshell.io.MessageWritableList;
import chatshell.rpcserver.RPCServerProtocol;

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
	public MessageWritableList getHistoryMessage(LongWritable beginTime) {
		List<MessageWritable> list = messageDao.findLatestMessageByTime(beginTime.getValue());
		MessageWritableList msgList = new MessageWritableList(list);
		return msgList;
	}

	public static void main(String[] args) throws BeanLoaderException {
		String pck[] = { "chatshell" };
		PraticalBeanFactory beanFactory = new PraticalBeanFactory(pck);
		ChatServer chatServer = beanFactory.getBean(ChatServer.class);
		Server rpcServer = RPC.getServer(chatServer, Chat.SERVER_HOST, Chat.SERVER_RPC_PORT);
		rpcServer.start();
	}
}
