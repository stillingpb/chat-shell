package chatshell;

import ioc.annotation.Component;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import rpc.io.LongWritable;
import rpc.ipc.RPC;
import chatshell.io.MessageWritable;
import chatshell.io.MessageWritableList;
import chatshell.io.SerializeUtil;
import chatshell.rpcserver.RPCServerProtocol;

@Component
public class ChatMonitor implements Runnable {
	private static String LOCAL_RECORD_FILE = "client.txt";
	private RandomAccessFile accessFile;

	private volatile boolean isRunning = true;
	private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

	private Socket socket;
	private DataInputStream dataIn;

	private RPCServerProtocol rpcServer;

	public ChatMonitor() {
		rpcServer = RPC.getClientProxy(RPCServerProtocol.class, Chat.SERVER_HOST,
				Chat.SERVER_RPC_PORT);
		try {
			File localFile = new File(LOCAL_RECORD_FILE);
			if (!localFile.exists())
				localFile.createNewFile();
			accessFile = new RandomAccessFile(localFile, "rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			socket = new Socket(Chat.SERVER_HOST, Chat.SERVER_MSG_PORT);
			dataIn = new DataInputStream(socket.getInputStream());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		getHistoryMessage();
		while (isRunning) {
			MessageWritable msgWritable = getMessage();
			outputMessage(msgWritable);
			recordLastMessageSendTime(msgWritable.getSendTime());
		}
	}

	private void getHistoryMessage() {
		long lastMsgSendTime = getLastMessageSendTime();
		MessageWritableList msgWritableList = rpcServer.getHistoryMessage(new LongWritable(
				lastMsgSendTime));
		List<MessageWritable> list = msgWritableList.getMsgList();
		for (MessageWritable msgWritable : list)
			outputMessage(msgWritable);
	}

	private void outputMessage(MessageWritable msgWritable) {
		long sendTime = msgWritable.getSendTime();
		String userName = msgWritable.getUserName();
		String msg = msgWritable.getMsg();
		System.out.println("\n" + dateFormat.format(new Date(sendTime)));
		System.out.println(userName + ">\t" + msg);
	}

	private MessageWritable getMessage() {
		MessageWritable msgWritable = null;
		try {
			int len = dataIn.readInt();
			byte[] bytes = new byte[len];
			dataIn.readFully(bytes);
			msgWritable = SerializeUtil.deserializeObj(bytes, new MessageWritable());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return msgWritable;
	}

	private long getLastMessageSendTime() {
		long sendTime = 0;
		try {
			sendTime = accessFile.readLong();
		} catch (EOFException e) {
			// do nothing
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sendTime;
	}

	private void recordLastMessageSendTime(long sendTime) {
		try {
			accessFile.seek(0);
			accessFile.writeLong(sendTime);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Thread(new ChatMonitor()).start();
	}
}
