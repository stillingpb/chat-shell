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

	@Inject
	private ReadHandler readHandler;
	@Inject
	private BroadcastHandler broadcastHandler;

	public ChatServer() {
		try {
			acceptSelector = Selector.open();
			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(acceptSelector, SelectionKey.OP_ACCEPT);
			serverSocketChannel.socket().bind(new InetSocketAddress(Chat.SERVER_PORT));
		} catch (IOException e) {
			e.printStackTrace();
		}
		new Thread(readHandler).start();
		new Thread(broadcastHandler).start();
	}

	@Override
	public void run() {
		while (isRunning) {
			try {
				acceptSelector.select();
				Set<SelectionKey> selectKeys = acceptSelector.selectedKeys();
				for (SelectionKey sk : selectKeys) {
					if (sk.isAcceptable())
						doAccept(sk);
				}
			} catch (IOException e) {
				e.printStackTrace();
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
			e.printStackTrace();
		}
	}

	static class Connection {
		String uname;
		Message message;
		SocketChannel sckChannel;

		/**
		 * 
		 * @param sckChannel
		 *            此时的sckChannel是阻塞的
		 */
		public Connection(SocketChannel sckChannel) {
			message = new Message();
			this.sckChannel = sckChannel;
			readUserName();
		}

		/**
		 * 从socket channel 中读消息
		 * 
		 * @return 是否读完
		 */
		public boolean readMessage() {
			return message.readMessageFromSocketChannel(sckChannel);
		}

		private void readUserName() {
			try {
				ByteBuffer unameLenBuf = ByteBuffer.allocate(4);
				int count = sckChannel.write(unameLenBuf);
				while (count != 4) {
					count += sckChannel.write(unameLenBuf);
				}
				unameLenBuf.flip();
				int unameLen = unameLenBuf.asIntBuffer().get();
				ByteBuffer unameBuf = ByteBuffer.allocate(unameLen);
				count = sckChannel.write(unameBuf);
				while (count != unameLen) {
					count += sckChannel.write(unameBuf);
				}
				unameBuf.flip();
				uname = unameBuf.asCharBuffer().toString();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public Message getMessage() {
			return message;
		}

		public void setMessage(Message message) {
			this.message = message;
		}

		public String getUname() {
			return uname;
		}

		public void setUname(String uname) {
			this.uname = uname;
		}

		public SocketChannel getSckChannel() {
			return sckChannel;
		}

		public void setSckChannel(SocketChannel sckChannel) {
			this.sckChannel = sckChannel;
		}
	}

	public static void main(String[] args) throws BeanLoaderException {
		String pck[] = { "chatday" };
		PraticalBeanFactory beanFactory = new PraticalBeanFactory(pck);
		ChatServer chatServer = beanFactory.getBean(ChatServer.class);
		new Thread(chatServer).start();
	}
}
