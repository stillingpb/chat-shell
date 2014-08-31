package chatday.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import rpc.io.Writable;

public class MessageWritable implements Writable {
	private String userName;
	private String timeStamp;
	private String msg;

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(userName);
		out.writeUTF(timeStamp);
		out.writeUTF(msg);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		userName = in.readUTF();
		timeStamp = in.readUTF();
		msg = in.readUTF();
	}

}
