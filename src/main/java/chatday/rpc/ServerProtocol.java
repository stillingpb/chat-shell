package chatday.rpc;

import chatday.io.MessageWritableList;

public interface ServerProtocol {
	public boolean clientRegister();

	public MessageWritableList getHistoryMessage();
}
