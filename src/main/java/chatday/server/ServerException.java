package chatday.server;

public class ServerException extends RuntimeException {
	public ServerException(String msg, Exception e) {
		super(msg, e);
	}
}
