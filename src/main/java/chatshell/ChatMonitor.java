package chatshell;

import ioc.annotation.Component;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class ChatMonitor implements Runnable {
	private boolean isRunning = true;

	@Override
	public void run() {
		try {
			Socket socket = new Socket(Chat.SERVER_HOST, Chat.SERVER_MSG_PORT);
			DataInputStream dataIn = new DataInputStream(socket.getInputStream());
			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
			while (isRunning) {
				int len = dataIn.readInt();
				System.out.println("\n" + dateFormat.format(new Date()));
				byte[] data = new byte[len];
				dataIn.readFully(data);
				System.out.println(new String(data));
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Thread(new ChatMonitor()).start();
	}
}
