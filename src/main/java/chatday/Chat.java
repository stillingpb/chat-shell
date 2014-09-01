package chatday;

import ioc.PraticalBeanFactory;
import ioc.util.BeanLoaderException;

import java.util.HashMap;
import java.util.Map;

import chatday.msgserver.MsgServer;

public class Chat {
	public static final String SERVER_HOST = "10.108.211.36";
	public static final int SERVER_MSG_PORT = 2345;
	public static final int SERVER_RPC_PORT = 2346;

	private static String pck[] = { "chatday" };
	private static PraticalBeanFactory beanFactory = new PraticalBeanFactory(pck);

	private static Map<String, Class<? extends Runnable>> alias;
	private static String usage;
	static {
		alias = new HashMap<String, Class<? extends Runnable>>();
		alias.put("c", ChatClient.class);
		alias.put("m", ChatMonitor.class);
		alias.put("s", MsgServer.class);

		usage = "correct usage : chat [param]\n" + "param:\n" + "c : Client\n" + "m : Monitor\n"
				+ "s : Server";
	}

	public static void main(String[] args) throws BeanLoaderException {
		if (args.length != 1 || alias.get(args[0]) == null) {
			System.out.println(usage);
			return;
		}
		Runnable obj = beanFactory.getBean(alias.get(args[0]));
		new Thread(obj).start();
	}
}
