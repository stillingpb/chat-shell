package chatday.server;

import ioc.annotation.Component;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import chatday.server.ChatServer.Connection;

@Component
@Singleton
public class ReadHandler implements Runnable {
	private Selector readSelector;
	private volatile boolean adding;

	@Inject
	private BroadcastHandler broadcastHandler;

	public ReadHandler() {
		try {
			readSelector = Selector.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SelectionKey registerChannel(SocketChannel socketChannel) {
		startRegisterChannel();
		SelectionKey sk = null;
		try {
			sk = socketChannel.register(readSelector, SelectionKey.OP_READ);
		} catch (ClosedChannelException e) {
			e.printStackTrace();
		} finally {
			finishRegisterChannel();
		}
		return sk;
	}

	private void startRegisterChannel() {
		adding = true;
		readSelector.wakeup();
	}

	private synchronized void finishRegisterChannel() {
		adding = false;
		this.notify();
	}

	@Override
	public void run() {
		while (ChatServer.isRunning) {
			try {
				readSelector.select();
				synchronized (this) {
					while (adding)
						try {
							this.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
				}
				Set<SelectionKey> selectKeys = readSelector.selectedKeys();
				for (SelectionKey sk : selectKeys) {
					if (sk.isReadable()) {
						Connection conn = (Connection) sk.attachment();
						boolean isReadOver = conn.readMessage();
						if (isReadOver) {
							Message msg = conn.getMessage();
							conn.setMessage(new Message());
							broadcastHandler.addMessage(msg);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
