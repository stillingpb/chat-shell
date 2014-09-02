package chatshell;

import ioc.PraticalBeanFactory;
import ioc.annotation.Component;
import ioc.util.BeanLoaderException;

import javax.inject.Inject;
import javax.inject.Singleton;

import rpc.ipc.RPC;
import rpc.ipc.server.ServerStub;
import chatshell.msgserver.MsgServer;
import chatshell.rpcserver.RPCServer;

@Component
@Singleton
public class ChatServer {
	// @Inject
	// private Provider<RPCServer> rpcProvider;
	// @Inject
	// private Provider<MsgServer> msgProvider;

	@Inject
	private RPCServer rpcServer;
	@Inject
	private MsgServer msgServer;

	public void startServer() {
		// rpcServer = rpcProvider.get();
		// msgServer = msgProvider.get();
		ServerStub rpcServerStub = RPC.getServer(rpcServer, Chat.SERVER_HOST, Chat.SERVER_RPC_PORT);
		rpcServerStub.start();
		new Thread(msgServer).start();
	}

	public static void main(String[] args) throws BeanLoaderException {
		String pck[] = { "chatshell" };
		PraticalBeanFactory beanFactory = new PraticalBeanFactory(pck);
		ChatServer chatServer = beanFactory.getBean(ChatServer.class);
		chatServer.startServer();
	}
}
