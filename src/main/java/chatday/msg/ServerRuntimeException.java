package chatday.msg;

public class ServerRuntimeException extends RuntimeException {
	public ServerRuntimeException(String msg, Exception e) {
		super(msg, e);
	}
}
