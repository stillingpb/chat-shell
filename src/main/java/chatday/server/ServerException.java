package chatday.server;

public class ServerException extends Exception {
	public ServerException(String msg, Exception e) {
		super(msg, e);
	}
}
