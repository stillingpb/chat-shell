package chatday.msgserver;

import ioc.annotation.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Singleton;

@Component
@Singleton
public class BroadcastHandler implements Runnable {

	private LinkedBlockingQueue<Message> msgQueue;
	/**
	 * 当前已连接的connection集.
	 */
	private Map<Connection, Integer> curConnections;// <connection, msg已写出字节数>
	// /**
	// * 两轮message消息发送中间，新添加的connection.
	// */
	// private Map<Connection, Integer> newConnections; // <connection,
	// msg已写出字节数>
	private Selector broadcastSelector;

	private volatile boolean adding;

	public BroadcastHandler() {
		this.msgQueue = new LinkedBlockingQueue<Message>();
		curConnections = new HashMap<Connection, Integer>();
		// newConnections = new HashMap<Connection, Integer>();
		try {
			broadcastSelector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addConnection(Connection conn) {
		startRegisterChannel();
		try {
			conn.getSckChannel().register(broadcastSelector, SelectionKey.OP_WRITE, conn);
			curConnections.put(conn, 0);
		} catch (ClosedChannelException e) {
			throw new ServerRuntimeException("向select注册write事件失败", e);
		} finally {
			finishRegisterChannel();
		}
	}

	private void updateConnections() {
		for (Connection conn : curConnections.keySet())
			curConnections.put(conn, 0);
	}

	private void startRegisterChannel() {
		adding = true;
		broadcastSelector.wakeup();
	}

	private synchronized void finishRegisterChannel() {
		adding = false;
		this.notify();
	}

	private synchronized void waitChannelRegister() {
		while (adding) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				throw new ServerRuntimeException("向select注册write事件失败", e);
			}
		}
	}

	public void addMessage(Message msg) {
		msgQueue.offer(msg);
	}

	@Override
	public void run() {
		while (MsgServer.isRunning) {
			Message msg = null;
			try {
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				throw new ServerRuntimeException("获取待发送msg失败", e);
			}
			processMessage(msg);
			updateConnections();
		}
	}

	/**
	 * 将一条消息广播给所有需要消息的客户端
	 * 
	 * @param msg
	 */
	private void processMessage(Message msg) {
		int sendOverCount = 0; // 记录发送消息完成的connection数
		int totalLen = msg.getLen() + 4;
		ByteBuffer lenBuf = msg.getLenBuf();
		ByteBuffer bodyBuf = msg.getBodyBuf();

		/* 将buffer 设置为读状态 */
		lenBuf.limit(4);
		bodyBuf.limit(msg.getLen());

		while (sendOverCount < curConnections.size()) {
			try {
				broadcastSelector.select();
			} catch (IOException e) {
				throw new ServerRuntimeException("broadcast发送失败", e);
			}
			waitChannelRegister();
			Set<SelectionKey> skSet = broadcastSelector.selectedKeys();
			for (SelectionKey sk : skSet) {
				Connection conn = (Connection) sk.attachment();
				int offset = curConnections.get(conn);
				if (sk.isWritable() && offset < totalLen) {
					try {
						offset += sendMessage(conn, lenBuf, bodyBuf, offset);
						curConnections.put(conn, offset);
						if (offset == totalLen) {
							sendOverCount++;
						}
					} catch (ServerException e) {// channel损坏，需要从selector中移除
						curConnections.remove(conn);
						sk.cancel();
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param conn
	 * @param lenBuf
	 * @param bodyBuf
	 * @param offset
	 * @return 返回发送的字节数
	 * @throws ServerException
	 */
	private int sendMessage(Connection conn, ByteBuffer lenBuf, ByteBuffer bodyBuf, int offset)
			throws ServerException {
		int count = 0;
		SocketChannel skChannel = conn.getSckChannel();
		try {
			if (offset < 4) {
				lenBuf.position(offset);// 从offset处开始读数据
				count = skChannel.write(lenBuf);
			}
			if (offset + count >= 4) {
				bodyBuf.position(offset + count - 4);// 从(offset+count-4)处开始读数据
				count += skChannel.write(bodyBuf);
			}
		} catch (IOException e) {
			throw new ServerException("broadcast发送失败", e);
		}
		return count;
	}
}
