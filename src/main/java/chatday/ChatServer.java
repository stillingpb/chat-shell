package chatday;

import ioc.annotation.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class ChatServer implements Runnable {
	private Set<SocketChannel> linkedClient;
	private Selector selector;
	private boolean isRunning = true;

	public ChatServer() {
		try {
			linkedClient = new HashSet<SocketChannel>();
			selector = Selector.open();

			ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.configureBlocking(false);
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			ServerSocket serverSocket = serverSocketChannel.socket();
			serverSocket.bind(new InetSocketAddress(Chat.SERVER_PORT));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (isRunning) {
			try {
				selector.select();
				Set<SelectionKey> selectKeys = selector.selectedKeys();
				for (SelectionKey sk : selectKeys) {
					if (sk.isAcceptable()) {
						doAccept(sk);
					} else if (sk.isReadable()) {
						doRead(sk);
					}
					selectKeys.remove(sk);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void doAccept(SelectionKey sk) {
		try {
			ServerSocketChannel serverSocketChannel = (ServerSocketChannel) sk.channel();
			SocketChannel socketChannel = serverSocketChannel.accept();
			socketChannel.configureBlocking(false);
			socketChannel.socket().setTcpNoDelay(true);
			socketChannel.register(selector, SelectionKey.OP_READ);
			linkedClient.add(socketChannel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 这种读数据的方式效率极低，没有发挥非阻塞读的优势，反而忙等。。。但如果设置为阻塞读，那么无法用Selector
	 * 
	 * @param sk
	 */
	private void doRead(SelectionKey sk) {
		try {
			SocketChannel socketChannel = (SocketChannel) sk.channel();
			ByteBuffer buff = ByteBuffer.allocate(512); // 单行数据长度不大
			socketChannel.read(buff);
			buff.flip();
			while (buff.remaining() < 4) { // 获取数据长度信息
				TimeUnit.MILLISECONDS.sleep(30);
				buff.compact();
				socketChannel.read(buff);
				buff.flip();
			}
			int dataLen = buff.getInt();
			byte[] data = new byte[dataLen];
			int cnt = 0;
			for (;;) {
				int remain = buff.remaining();
				remain = Math.min(remain, dataLen - cnt);
				buff.get(data, cnt, remain);
				cnt += remain;
				if (cnt >= dataLen)
					break;
				buff.clear();
				socketChannel.read(buff);
				buff.flip();
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
			System.out.println("\n" + dateFormat.format(new Date()));
			System.out.println(new String(data));
			notifyChatClient(data);

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	private void notifyChatClient(byte[] data) {
		for (SocketChannel sc : linkedClient) {
			try {
				if (sc.isOpen()) {
					ByteBuffer lenBuff = (ByteBuffer) ByteBuffer.allocate(4).putInt(data.length)
							.flip();
					ByteBuffer dataBuff = ByteBuffer.wrap(data);
					ByteBuffer[] buff = { lenBuff, dataBuff };
					int remain = lenBuff.remaining() + dataBuff.remaining();
					while (remain > 0) {
						remain -= sc.write(buff);
					}
				} else {
					sc.close();
					linkedClient.remove(sc);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
