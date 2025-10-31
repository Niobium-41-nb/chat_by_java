import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 客户端管理器类 - 用于线程间通信
 */
class ClientManager {
    private static List<PrintWriter> clients = new ArrayList<>();
    private static Map<String,PrintWriter> Map_User;
	private static Map<PrintWriter,String> to_User;
	private static int idx = 0;
	static {
		Map_User = Collections.synchronizedMap(new HashMap<>());
	}
	static {
		to_User = Collections.synchronizedMap(new HashMap<>());
	}
    /**
     * 添加客户端输出流
     * @param out 客户端输出流
     */
    public static synchronized String addClient(PrintWriter out) {
		String username = "user"+idx;idx++;
		Map_User.put(username, out);
		to_User.put(out,username);
        clients.add(out);
		return username;
    }
    
    /**
     * 移除客户端输出流
     * @param out 客户端输出流
     */
    public static synchronized void removeClient(PrintWriter out) {
        clients.remove(out);
        String username = to_User.get(out);
        if (username != null) {
            Map_User.remove(username);
        }
        to_User.remove(out);
    }
    
    /**
     * 私发消息
     * @param message 要私发的消息
     * @param excludeClient 要发送的客户端
     */
    public static synchronized void sicast(String message, String to_username) {
        PrintWriter targetClient = Map_User.get(to_username);
        if (targetClient != null) {
            targetClient.println(message);
            targetClient.flush();
        }
    }
	/**
     * 广播消息
     * @param message 要私发的消息
     * @param excludeClient 要不发送的客户端
     */
	public static synchronized void broadcast(String message, PrintWriter excludeClient) {
        for (PrintWriter client : clients) {
            if (client != excludeClient) {
                client.println(message);
                client.flush();
            }
        }
    }
    
    /**
     * 获取当前客户端数量
     * @return 客户端数量
     */
    public static synchronized int getClientCount() {
        return clients.size();
    }
}

/**
 * 多线程聊天服务器类
 * 支持多个客户端同时连接并进行聊天
 */
public class MultiTalkServer
{
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
        } catch (IOException e){
            System.err.println("无法在端口8898上监听。");
            System.exit(-1);
        }

        // 主循环：持续监听客户端连接
        while (listening){
			Socket socket;
			// 等待客户端连接
			socket = serverSocket.accept();
			clientNumber++;  // 增加客户端计数
			// 为每个客户端创建新的线程处理
			new MultiTalkServerThread(socket, clientNumber).start();
		}
        // 关闭服务器套接字
        serverSocket.close();
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

	
	/**
	 * 判断消息发给谁，提取@后面的人名
	 * @param Message 输入的消息字符串
	 * @return 提取到的用户名，如果没有找到则返回null
	 */
	private static String to_who(String Message) {
		// 检查消息是否为空
		if (Message == null || Message.trim().isEmpty()) {
			return null;
		}
		
		// 查找@符号的位置
		int atIndex = Message.indexOf('@');
		
		// 如果没有找到@符号，返回null
		if (atIndex == -1) {
			return null;
		}
		
		// 从@符号后面开始查找用户名的结束位置（空格、标点符号或字符串末尾）
		int endIndex = atIndex + 1;
		while (endIndex < Message.length()) {
			char c = Message.charAt(endIndex);
			// 判断字符是否为字母、数字或下划线（用户名的合法字符）
			if (!Character.isLetterOrDigit(c) && c != '_') {
				break;
			}
			endIndex++;
		}
		
		// 如果@后面没有有效字符，返回null
		if (endIndex == atIndex + 1) {
			return null;
		}
		
		// 提取并返回@后面的用户名
		return Message.substring(atIndex + 1, endIndex);
	}
	/**
	 * 构造函数
	 * @param socket 客户端套接字
	 * @param clientNumber 客户端编号
	 */
	public MultiTalkServerThread(Socket socket, int clientNumber)
	{
		super("MultiTalkServerThread");  // 设置线程名称
		this.socket = socket;
		this.clientNumber = clientNumber;
		System.out.println("接受客户端" + clientNumber + "连接");
	}

	/**
	 * 线程执行方法
	 * 处理与客户端的通信
	 */
	public void run()
	{
		PrintWriter out = null;
		try {
			// 创建输出流，用于向客户端发送数据
			out = new PrintWriter(socket.getOutputStream(), true);  // 自动刷新
			// 创建输入流，用于接收客户端数据
			BufferedReader in = new BufferedReader(
						new InputStreamReader(
						socket.getInputStream()));

			// 将客户端添加到管理器
			String new_user = ClientManager.addClient(out);
			
			// 通知所有客户端有新用户加入
			String joinMessage = "系统消息: 客户端" + new_user + "加入了聊天室，当前在线人数: " + ClientManager.getClientCount();
			System.out.println(joinMessage);
			ClientManager.broadcast(joinMessage, out);

			String inputLine;
			boolean inbye = false;         // 客户端结束标志
			        

			// 通信循环
			do
			{
				// 读取客户端信息
				inputLine = in.readLine();
				
				// 检查连接是否断开
				if (inputLine == null) {
					System.out.println("客户端" + new_user + "断开连接");
					break;
				}
				
				System.out.println("来自客户端" + new_user + ": " + inputLine);
				
				// 如果客户端发送"Bye."，则结束对话
				if(inputLine.equals("Bye."))
				{
					inbye = true;           // 设置客户端结束标志
					
					// 通知所有客户端有用户离开
					String leaveMessage = "系统消息: 客户端" + new_user + "离开了聊天室，当前在线人数: " + (ClientManager.getClientCount() - 1);
					System.out.println(leaveMessage);
					ClientManager.broadcast(leaveMessage, out);
					
					break;
				}
				else
				{
					String to_username = to_who(inputLine);
					if(to_username == null){
						// 广播客户端消息给所有其他客户端
						String broadcastMessage = "客户端" + new_user + "说: " + inputLine;
						System.out.println(broadcastMessage);
						ClientManager.broadcast(broadcastMessage, out);
					}else{
						String broadcastMessage = "客户端" + new_user + "对" + to_username + "说: " + inputLine;
						System.out.println(broadcastMessage);
						ClientManager.sicast(broadcastMessage, to_username);
						
						// 同时给发送者返回确认消息
						out.println("你私聊" + to_username + ": " + inputLine.replace("@" + to_username, ""));
						out.flush();
					}
				}
			} while(!inbye);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// 从管理器移除客户端
			if (out != null) {
				ClientManager.removeClient(out);
			}
			
			// 关闭所有流和套接字
			try {
				if (socket != null && !socket.isClosed()) {
					socket.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("客户端" + clientNumber + "连接已关闭");
		}
	   }
}
// 