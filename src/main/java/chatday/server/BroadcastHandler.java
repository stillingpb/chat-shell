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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Singleton;

import chatday.server.ChatServer.Connection;

@Component
@Singleton
public class BroadcastHandler implements Runnable {

	LinkedBlockingQueue<Message> msgQueue;
	Map<Connection, Integer> curConnections;
	Map<Connection, Integer> newConnections;
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
			e.printStackTrace();
		} finally {
			finishRegisterChannel();
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
				e.printStackTrace();
			}
			processMessage(msg);
			updateConnections();
		}
	}

	private void updateConnections() {
		for (Entry<Connection, Integer> entry : curConnections.entrySet())
			entry.setValue(0);
		synchronized (newConnections) {
			curConnections.putAll(newConnections);
			newConnections.clear();
		}
	}

	private void processMessage(Message msg) {
		int connCount = curConnections.size();
		int sendOverCount = 0;
		int msgLen = msg.len + 4;
		ByteBuffer lenBuf = msg.lenBuf;
		ByteBuffer bodyBuf = msg.bodyBuf;
		lenBuf.position(0);
		lenBuf.limit(4);
		bodyBuf.position(0);
		bodyBuf.limit(msg.len);
		while (sendOverCount != connCount) {
			try {
				broadcastSelector.select();
				synchronized (this) {
					while (adding)
						try {
							this.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
				}
				Set<SelectionKey> skSet = broadcastSelector.selectedKeys();
				for (SelectionKey sk : skSet) {
					Connection conn = (Connection) sk.attachment();
					int offset = curConnections.get(conn);
					if (sk.isWritable() && offset < msgLen) {
						offset += sendMessage(conn, lenBuf, bodyBuf, offset);
						curConnections.put(conn, offset);
						if (offset == msgLen) {
							sendOverCount++;
							conn.getSckChannel();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
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
			lenBuf.position(offset);
			try {
				count = skChannel.write(lenBuf);
				offset += count;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (offset >= 4) {
			bodyBuf.position(offset - 4);
			try {
				count += skChannel.write(bodyBuf);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return count;
	}
}
