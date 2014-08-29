package chatday.server;

import ioc.PraticalBeanFactory;
import ioc.annotation.Component;
import ioc.util.BeanLoaderException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

import javax.inject.Inject;

import chatday.Chat;

@Component
public class ChatServer implements Runnable {
	private Selector acceptSelector;
	static volatile boolean isRunning = true;

	private ReadHandler readHandler;
	private BroadcastHandler broadcastHandler;

	@Inject
	public ChatServer(ReadHandler readHandler, BroadcastHandler broadcastHandler) {
		try {
			acceptSelector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
			serverSocketChannel.socket().bind(new InetSocketAddress(Chat.SERVER_PORT));
		} catch (IOException e) {
			throw new ServerException("chat server初始化失败", e);
		}
		this.readHandler = readHandler;
		this.broadcastHandler = broadcastHandler;
		new Thread(readHandler).start();
		new Thread(broadcastHandler).start();
	}

	@Override
	public void run() {
		while (isRunning) {
			try {
				acceptSelector.select();
			} catch (IOException e) {
				throw new ServerException("accept监听失败", e);
			}
			Set<SelectionKey> selectKeys = acceptSelector.selectedKeys();
			for (SelectionKey sk : selectKeys) {
				if (sk.isAcceptable())
					doAccept(sk);
			}
		}
	}

	private void doAccept(SelectionKey sk) {
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) sk.channel();
		try {
			SocketChannel socketChannel = serverSocketChannel.accept();
			Connection conn = new Connection(socketChannel);
			socketChannel.configureBlocking(false);
			socketChannel.socket().setTcpNoDelay(true);
			SelectionKey sKey = readHandler.registerChannel(socketChannel);
			sKey.attach(conn);
			broadcastHandler.addConnection(conn);
		} catch (IOException e) {
			// 当前连接服务器的socketchannel发生异常，跳过该异常就行了
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws BeanLoaderException {
		String pck[] = { "chatday" };
		PraticalBeanFactory beanFactory = new PraticalBeanFactory(pck);
		ChatServer chatServer = beanFactory.getBean(ChatServer.class);
		new Thread(chatServer).start();
	}
}
