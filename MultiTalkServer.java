import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 多线程聊天服务器类
 * 支持多个客户端同时连接并进行聊天，具有广播功能
 */
public class MultiTalkServer
{
    // 使用线程安全的集合存储所有客户端线程
    public static CopyOnWriteArrayList<MultiTalkServerThread> clientList = new CopyOnWriteArrayList<>();
    
    /**
     * 服务器主方法
     * @param args 命令行参数
     * @throws IOException 输入输出异常
     */
    public static void main(String[] args) throws IOException
	{
        ServerSocket serverSocket = null;        // 服务器套接字
        boolean listening = true;                // 服务器监听标志
		int clientNumber = 0;                   // 客户端计数器

        // 创建服务器套接字，监听8898端口
        try {
            serverSocket = new ServerSocket(8898);
            System.out.println("多线程聊天服务器已启动，正在监听端口8898...");
        }
        catch (IOException e){
            System.err.println("无法在端口8898上监听。");
            System.exit(-1);
        }

        // 启动服务器控制台输入线程，用于发送广播消息
        new Thread(new ServerConsole()).start();

        // 主循环：持续监听客户端连接
        while (listening){
			Socket socket = serverSocket.accept();
			clientNumber++;
			String clientIP = socket.getInetAddress().getHostAddress();
			// 为每个客户端创建新的线程处理
			MultiTalkServerThread clientThread = new MultiTalkServerThread(socket, clientNumber, clientIP);
			clientList.add(clientThread);
			clientThread.start();
			
			// 广播新客户端加入信息
			broadcastMessage("服务器", "客户端" + clientNumber + "(IP: " + clientIP + ") 已加入聊天室");
		}
        // 关闭服务器套接字
        serverSocket.close();
    }
    
    /**
     * 广播消息给所有客户端
     * @param sender 发送者
     * @param message 消息内容
     */
    public static void broadcastMessage(String sender, String message) {
        String formattedMessage = "[" + sender + "]: " + message;
        System.out.println(formattedMessage);
        
        // 遍历所有客户端，发送消息
        for (MultiTalkServerThread client : clientList) {
            client.sendMessage(formattedMessage);
        }
    }
    
    /**
     * 从客户端列表中移除指定客户端
     * @param client 要移除的客户端线程
     */
    public static void removeClient(MultiTalkServerThread client) {
        clientList.remove(client);
        // 广播客户端离开信息
        broadcastMessage("服务器", "客户端" + client.getClientNumber() + "(IP: " + client.getClientIP() + ") 已离开聊天室");
    }
    
    /**
     * 服务器控制台线程类，用于从服务器控制台发送广播消息
     */
    static class ServerConsole implements Runnable {
        @Override
        public void run() {
            try {
                BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
                String serverMessage;
                
                System.out.println("服务器控制台已启动，可以输入消息进行广播（输入'quit'退出）");
                
                while ((serverMessage = consoleInput.readLine()) != null) {
                    if (serverMessage.equalsIgnoreCase("quit")) {
                        System.out.println("服务器控制台已关闭");
                        break;
                    }
                    // 广播服务器消息
                    broadcastMessage("服务器", serverMessage);
                }
                
                consoleInput.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

/**
 * 多线程聊天服务器线程类
 * 每个客户端连接都会创建一个独立的线程来处理通信
 */
class MultiTalkServerThread extends Thread
{
	private Socket socket = null;        // 客户端套接字
	private int clientNumber;            // 客户端编号
	private String clientIP;             // 客户端IP地址
	private PrintWriter out = null;      // 输出流

	/**
	 * 构造函数
	 * @param socket 客户端套接字
	 * @param clientNumber 客户端编号
	 * @param clientIP 客户端IP地址
	 */
	public MultiTalkServerThread(Socket socket, int clientNumber, String clientIP)
	{
		super("MultiTalkServerThread");  // 设置线程名称
		this.socket = socket;
		this.clientNumber = clientNumber;
		this.clientIP = clientIP;
		System.out.println("接受客户端" + clientNumber + "(IP: " + clientIP + ") 连接");
	}

	/**
	 * 线程执行方法
	 * 处理与客户端的通信
	 */
	public void run()
	{
		try {
			// 创建输出流，用于向客户端发送数据
			out = new PrintWriter(socket.getOutputStream(), true);  // 自动刷新
			// 创建输入流，用于接收客户端数据
			BufferedReader in = new BufferedReader(
						new InputStreamReader(
						socket.getInputStream()));

			String inputLine;
			boolean inbye = false;
			         
			// 向新连接的客户端发送欢迎消息
			sendMessage("欢迎加入聊天室！当前在线人数: " + MultiTalkServer.clientList.size());
			sendMessage("输入'Bye.'退出聊天室");
			         
			// 通信循环
			while (!inbye && (inputLine = in.readLine()) != null) {
				if (inputLine.equals("Bye.")) {
					inbye = true;
					sendMessage("Bye.");
				}
				else {
					// 广播客户端消息
					MultiTalkServer.broadcastMessage("客户端" + clientNumber + "(" + clientIP + ")", inputLine);
				}
			}

		} catch (IOException e) {
			System.err.println("与客户端" + clientNumber + "(IP: " + clientIP + ") 通信出错: " + e.getMessage());
		} finally {
			// 关闭所有流和套接字
			try {
				if (out != null) out.close();
				if (socket != null && !socket.isClosed()) socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// 从客户端列表中移除
			MultiTalkServer.removeClient(this);
			System.out.println("客户端" + clientNumber + "(IP: " + clientIP + ") 连接已关闭");
		}
    }
    
    /**
     * 发送消息给当前客户端
     * @param message 要发送的消息
     */
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush();
        }
    }
    
    /**
     * 获取客户端编号
     * @return 客户端编号
     */
    public int getClientNumber() {
        return clientNumber;
    }
    
    /**
     * 获取客户端IP地址
     * @return 客户端IP地址
     */
    public String getClientIP() {
        return clientIP;
    }
}
