package chatday.rpcserver;

import chatday.io.MessageWritableList;

public interface RPCServerProtocol {
	public boolean clientRegister();

	public MessageWritableList getHistoryMessage(long beginTime);
}
