package chatday.server;

import java.nio.ByteBuffer;

class Message {
	private int len;
	private ByteBuffer lenBuf;
	private ByteBuffer bodyBuf;

	public Message() {
		lenBuf = ByteBuffer.allocate(4);
	}

	public int getLen() {
		return len;
	}

	public void setLen(int len) {
		this.len = len;
	}

	public ByteBuffer getLenBuf() {
		return lenBuf;
	}

	public void setLenBuf(ByteBuffer lenBuf) {
		this.lenBuf = lenBuf;
	}

	public ByteBuffer getBodyBuf() {
		return bodyBuf;
	}

	public void setBodyBuf(ByteBuffer bodyBuf) {
		this.bodyBuf = bodyBuf;
	}
}
