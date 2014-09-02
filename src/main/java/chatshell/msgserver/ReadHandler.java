package chatshell.msgserver;

import ioc.annotation.Component;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import chatshell.dao.MessageDao;
import chatshell.io.MessageWritable;
import chatshell.io.SerializeUtil;

@Component
@Singleton
public class ReadHandler implements Runnable {
	private Selector readSelector;
	private volatile boolean adding;

	@Inject
	@Named("MessageDaoImpl")
	MessageDao msgDao;

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
			throw new ServerRuntimeException("向select注册read事件失败", e);
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

	private synchronized void waitChannelRegister() {
		while (adding) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				throw new ServerRuntimeException("向select注册read事件失败", e);
			}
		}
	}

	@Override
	public void run() {
		while (MsgServer.isRunning) {
			try {
				readSelector.select();
			} catch (IOException e) {
				throw new ServerRuntimeException("read监听失败", e);
			}
			waitChannelRegister();
			Set<SelectionKey> selectKeys = readSelector.selectedKeys();
			for (SelectionKey sk : selectKeys) {
				if (sk.isReadable()) {
					Connection conn = (Connection) sk.attachment();
					try {
						boolean isReadOver = conn.readMessage();
						if (isReadOver) {
							Message msg = conn.getMsg();
							conn.setMessage(new Message());
							storeMessageToDataBase(msg);
							broadcastHandler.addMessage(msg);
						}
					} catch (ServerException e) { // channel损坏，需要从selector中移除
						sk.cancel();
					}
				}
			}
		}
	}

	private void storeMessageToDataBase(Message msg) {
		byte[] bytes = msg.getBodyBuf().array();
		MessageWritable msgWritable = SerializeUtil.deserializeObj(bytes, new MessageWritable());
		msgDao.addMessage(msgWritable);
	}
}
