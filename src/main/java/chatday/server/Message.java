package chatday.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

class Message {
	int len;
	// String body;
	ByteBuffer lenBuf;
	ByteBuffer bodyBuf;

	public Message() {
		lenBuf = ByteBuffer.allocate(4);
	}

	int lenCount;
	int bodyCount;

	/**
	 * 
	 * @param sckChannel
	 * @return 读完一条数据，返回true，没读完，返回false
	 */
	public boolean readMessageFromSocketChannel(SocketChannel sckChannel) {
		try {
			if (lenCount < 4)
				lenCount += sckChannel.read(lenBuf);
			if (lenCount < 4)
				return false;
			if (lenCount == 4) {
				lenBuf.flip();
				len = lenBuf.asIntBuffer().get();
				bodyBuf = ByteBuffer.allocate(len);
				lenCount = 5; // large than 4
			}
			if (bodyCount < len)
				bodyCount += sckChannel.read(bodyBuf);
			System.out.println(new String(bodyBuf.array()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (bodyCount != len)
			return false;
		else
			return true;
	}
}
