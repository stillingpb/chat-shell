package chatshell.rpcserver;

import rpc.io.LongWritable;
import chatshell.io.MessageWritableList;

public interface RPCServerProtocol {
	public boolean clientRegister();

	public MessageWritableList getHistoryMessage(LongWritable beginTime);
}
