package chatshell.rpcserver;

import ioc.PraticalBeanFactory;
import ioc.annotation.Component;
import ioc.util.BeanLoaderException;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import rpc.io.LongWritable;
import rpc.ipc.RPC;
import rpc.ipc.server.ServerStub;
import chatshell.Chat;
import chatshell.ChatServer;
import chatshell.dao.MessageDao;
import chatshell.io.MessageWritable;
import chatshell.io.MessageWritableList;

@Component
@Singleton
public class RPCServer implements RPCServerProtocol {

	@Inject
	@Named("messageDaoImpl")
	private MessageDao messageDao;

	@Override
	public MessageWritableList getHistoryMessage(LongWritable beginTime) {
		List<MessageWritable> list = messageDao.findLatestMessageByTime(beginTime.getValue());
		MessageWritableList msgList = new MessageWritableList(list);
		return msgList;
	}

	public static void main(String[] args) throws BeanLoaderException {
		String pck[] = { "chatshell" };
		PraticalBeanFactory beanFactory = new PraticalBeanFactory(pck);
		RPCServer rpcServer = beanFactory.getBean(RPCServer.class);
		ServerStub rpcServerStub = RPC.getServer(rpcServer, Chat.SERVER_HOST, Chat.SERVER_RPC_PORT);
		rpcServerStub.start();
	}
}
