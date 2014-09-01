package chatshell;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import rpc.io.LongWritable;
import rpc.ipc.RPC;
import chatshell.Chat;
import chatshell.io.MessageWritable;
import chatshell.io.MessageWritableList;
import chatshell.rpcserver.RPCServerProtocol;

public class testRPCServer {
	RPCServerProtocol rpcServer;

	@Before
	public void init() throws MalformedURLException, URISyntaxException {
		rpcServer = RPC.getClientProxy(RPCServerProtocol.class, Chat.SERVER_HOST,
				Chat.SERVER_RPC_PORT);
	}

	@Test
	public void testGetHistoryMessage() {
		MessageWritableList msgList = rpcServer.getHistoryMessage(new LongWritable(1));
		for (MessageWritable msg : msgList.getMsgList())
			System.out.println(msg);
	}
}
