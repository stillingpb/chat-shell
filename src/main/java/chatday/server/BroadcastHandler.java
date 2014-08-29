package chatday.server;

import ioc.annotation.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Singleton;

@Component
@Singleton
public class BroadcastHandler implements Runnable {

	LinkedBlockingQueue<Message> msgQueue;
	/**
	 * 当前已连接的connection集.
	 */
	Map<Connection, Integer> curConnections;// <connection, msg已写出字节数>
	/**
	 * 两轮message消息发送中间，新添加的connection.
	 */
	Map<Connection, Integer> newConnections; // <connection, msg已写出字节数>
	Selector broadcastSelector;

	private volatile boolean adding;

	public BroadcastHandler() {
		this.msgQueue = new LinkedBlockingQueue<Message>();
		curConnections = new HashMap<Connection, Integer>();
		newConnections = new HashMap<Connection, Integer>();
		try {
			broadcastSelector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void addConnection(Connection conn) {
		synchronized (newConnections) {
			newConnections.put(conn, 0);
		}
		startRegisterChannel();
		try {
			conn.getSckChannel().register(broadcastSelector, SelectionKey.OP_WRITE, conn);
		} catch (ClosedChannelException e) {
			throw new ServerException("向select注册write事件失败", e);
		} finally {
			finishRegisterChannel();
		}
	}

	private void updateConnections() {
		for (Connection conn : curConnections.keySet())
			curConnections.put(conn, 0);
		synchronized (newConnections) {
			curConnections.putAll(newConnections);
			newConnections.clear();
		}
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
				throw new ServerException("向select注册write事件失败", e);
			}
		}
	}

	public void addMessage(Message msg) {
		msgQueue.offer(msg);
	}

	@Override
	public void run() {
		while (ChatServer.isRunning) {
			Message msg = null;
			try {
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				throw new ServerException("获取待发送msg失败", e);
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
		int connCount = curConnections.size();
		int sendOverCount = 0; // 记录发送消息完成的connection数
		int totalLen = msg.getLen() + 4;
		ByteBuffer lenBuf = msg.getLenBuf();
		ByteBuffer bodyBuf = msg.getBodyBuf();

		/* 将buffer 设置为读状态 */
		lenBuf.limit(4);
		bodyBuf.limit(msg.getLen());

		while (sendOverCount != connCount) {
			try {
				broadcastSelector.select();
			} catch (IOException e) {
				throw new ServerException("broadcast发送失败", e);
			}
			waitChannelRegister();
			Set<SelectionKey> skSet = broadcastSelector.selectedKeys();
			for (SelectionKey sk : skSet) {
				Connection conn = (Connection) sk.attachment();
				int offset = curConnections.get(conn);
				if (sk.isWritable() && offset < totalLen) {
					offset += sendMessage(conn, lenBuf, bodyBuf, offset);
					curConnections.put(conn, offset);
					if (offset == totalLen) {
						sendOverCount++;
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
	 */
	private int sendMessage(Connection conn, ByteBuffer lenBuf, ByteBuffer bodyBuf, int offset) {
		int count = 0;
		SocketChannel skChannel = conn.getSckChannel();
		if (offset < 4) {
			lenBuf.position(offset);// 从offset处开始读数据
			try {
				count = skChannel.write(lenBuf);
			} catch (IOException e) {
				throw new ServerException("broadcast发送失败", e);
			}
		}
		if (offset + count >= 4) {
			bodyBuf.position(offset + count - 4);// 从(offset+count-4)处开始读数据
			try {
				count += skChannel.write(bodyBuf);
			} catch (IOException e) {
				throw new ServerException("broadcast发送失败", e);
			}
		}
		return count;
	}
}
