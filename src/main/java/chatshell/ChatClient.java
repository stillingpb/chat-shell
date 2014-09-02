package chatshell;

import ioc.annotation.Component;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import chatshell.io.MessageWritable;
import chatshell.io.SerializeUtil;

@Component
public class ChatClient implements Runnable {
	private static String USER_NAME = System.getProperty("user.name");
	private Socket socket;
	private DataOutputStream dataOut;
	private Scanner consoleReader;
	public ChatClient() {
		consoleReader = new Scanner(System.in);
		try {
			socket = new Socket(Chat.SERVER_HOST, Chat.SERVER_MSG_PORT);
			dataOut = new DataOutputStream(socket.getOutputStream());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		System.out.print("\nchat > ");
		while (consoleReader.hasNextLine()) {
			String line = consoleReader.nextLine();
			sendMsg(line);
			System.out.print("\nchat > ");
		}
	}

	private void sendMsg(String line) {
		long sendTime = System.currentTimeMillis();
		MessageWritable msgWritable = new MessageWritable(sendTime, USER_NAME, line);
		byte[] bytes = SerializeUtil.serializeObj(msgWritable);
		try {
			dataOut.writeInt(bytes.length);
			dataOut.write(bytes);
			dataOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Thread(new ChatClient()).start();
	}
}
