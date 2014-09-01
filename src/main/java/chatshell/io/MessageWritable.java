package chatshell.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import rpc.io.Writable;

public class MessageWritable implements Writable {
	private Long sendTime;
	private String userName;
	private String msg;

	public MessageWritable() {
	}

	public MessageWritable(long sendTime, String userName, String msg) {
		this.sendTime = sendTime;
		this.userName = userName;
		this.msg = msg;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(sendTime);
		out.writeUTF(userName);
		out.writeUTF(msg);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		sendTime = in.readLong();
		userName = in.readUTF();
		msg = in.readUTF();
	}

	public Long getSendTime() {
		return sendTime;
	}

	public void setSendTime(Long sendTime) {
		this.sendTime = sendTime;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String toString() {
		return "userName:" + userName + " sendTime:" + sendTime + " msg:" + msg;
	}
}
