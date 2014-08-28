package chatday;

import ioc.annotation.Component;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

@Component
public class ChatClient implements Runnable {
	private static String USER_NAME = System.getProperty("user.name") + " >\t";

	@Override
	public void run() {
		try {
			Socket socket = new Socket(Chat.SERVER_HOST, Chat.SERVER_PORT);
			DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream());
			Scanner consoleReader = new Scanner(System.in);
			System.out.print("\nchat > ");
			while (consoleReader.hasNextLine()) {
				String line = USER_NAME + consoleReader.nextLine();
				byte[] data = line.getBytes();
				dataOut.writeInt(data.length);
				dataOut.write(data);
				dataOut.flush();
				System.out.print("\nchat > ");
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new Thread(new ChatClient()).start();
	}
}
