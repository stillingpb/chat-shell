package chatday.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Connection {
	String uname; // head
	SocketChannel sckChannel;

	/**
	 * 
	 * @param sckChannel
	 *            此时的sckChannel是阻塞的
	 */
	public Connection(SocketChannel sckChannel) {
		msg = new Message();
		this.sckChannel = sckChannel;
//		readUserName();
	}

	/**
	 * 客户端刚连接服务器时，读取一些连接信息
	 */
	private void readUserName() {
		try {
			ByteBuffer unameLenBuf = ByteBuffer.allocate(4);
			int count = 0;
			do {
				count += sckChannel.write(unameLenBuf);
			} while (count < 4);
			unameLenBuf.flip();
			int unameLen = unameLenBuf.asIntBuffer().get();
			ByteBuffer unameBuf = ByteBuffer.allocate(unameLen);
			count = 0;
			do {
				count += sckChannel.write(unameBuf);
			} while (count < unameLen);
			unameBuf.flip();
			uname = unameBuf.asCharBuffer().toString();
		} catch (IOException e) {
			throw new ServerRuntimeException("读取连接相关消息发生异常", e);
		}
	}

	private Message msg;
	private int msgLen;
	private int msgLenCount;
	private int msgBodyCount;

	/**
	 * 从socket channel 中读消息
	 * 
	 * @return 读完一条数据，返回true，没读完，返回false
	 * @throws ServerException
	 */
	public boolean readMessage() throws ServerException {
		boolean isReadOver = readMessageFromSocketChannel(sckChannel, msg.getLenBuf(),
				msg.getBodyBuf());

		/* 为下一条message读取做准备 */
		if (isReadOver) {
			msg.setLen(msgLen);
			msgLenCount = 0;
			msgBodyCount = 0;
		}
		return isReadOver;
	}

	/**
	 * 
	 * @param sckChannel
	 * @return 读完一条数据，返回true，没读完，返回false
	 * @throws ServerException
	 */
	public boolean readMessageFromSocketChannel(SocketChannel sckChannel, ByteBuffer lenBuf,
			ByteBuffer bodyBuf) throws ServerException {
		try {
			if (msgLenCount < 4)
				msgLenCount += sckChannel.read(lenBuf);
			if (msgLenCount < 4)
				return false;
			if (msgLenCount == 4) {
				lenBuf.flip();
				msgLen = lenBuf.asIntBuffer().get();
				bodyBuf = ByteBuffer.allocate(msgLen);
				msg.setBodyBuf(bodyBuf);
				msgLenCount = 5; // large than 4
			}
			if (msgBodyCount < msgLen)
				msgBodyCount += sckChannel.read(bodyBuf);
			System.out.println(new String(bodyBuf.array()));
		} catch (IOException e) {
			throw new ServerException("读取message消息发生异常", e);
		}
		if (msgBodyCount != msgLen)
			return false;
		else
			return true;
	}

	public Message getMsg() {
		return msg;
	}

	public void setMessage(Message msg) {
		this.msg = msg;
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