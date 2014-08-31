package chatday.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rpc.io.Writable;

public class MessageWritableList implements Writable {

	private List<MessageWritable> msgList;

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(msgList.size());
		for (MessageWritable msg : msgList)
			msg.write(out);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int size = in.readInt();
		msgList = new ArrayList<MessageWritable>(size);
		for (int i = 0; i < size; i++) {
			MessageWritable msg = new MessageWritable();
			msg.readFields(in);
			msgList.add(msg);
		}
	}

}
