import java.io.*;
import java.net.*;

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
			String clientIP = socket.getInetAddress().getHostAddress();
			// 为每个客户端创建新的线程处理
			new MultiTalkServerThread(socket, clientNumber,clientIP).start();
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
	private String clientIP; // 客户端IP地址

	/**
	 * 构造函数
	 * @param socket 客户端套接字
	 * @param clientNumber 客户端编号
	 */
	public MultiTalkServerThread(Socket socket, int clientNumber,String clientIP)
	{
		super("MultiTalkServerThread");  // 设置线程名称
		this.socket = socket;
		this.clientNumber = clientNumber;
		System.out.println("接受客户端" + clientNumber + "连接");
		this.clientIP = clientIP;
	}

	/**
	 * 线程执行方法
	 * 处理与客户端的通信
	 */
	public void run()
	{
		try {
			// 创建输出流，用于向客户端发送数据
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);  // 自动刷新
			// 创建输入流，用于接收客户端数据
			BufferedReader in = new BufferedReader(
						new InputStreamReader(
						socket.getInputStream()));

			// 创建标准输入流，用于从控制台读取服务器输入
			BufferedReader sin = new BufferedReader( new InputStreamReader( System.in ) );

			String sinputLine, inputLine;  // 服务器输入和客户端输入
			boolean sinbye = false;        // 服务器结束标志
			boolean inbye = false;         // 客户端结束标志
			        
			// 读取客户端的第一条消息
			inputLine = in.readLine();
			// System.out.println( "来自客户端" + clientNumber + ": " + inputLine );
			
			// 通信循环
			do
			{
				// 如果客户端发送"Bye."，则结束对话
				if(inputLine.equals("Bye."))
				{
					inbye = true;           // 设置客户端结束标志
					sinbye = true;          // 设置服务器结束标志
					sinputLine = "Bye.";    // 准备结束消息
					out.println(sinputLine); // 发送结束消息
					out.flush();            // 刷新输出流
					System.out.println("服务器: " + "回复客户端 " + clientNumber +" : " + sinputLine);
				}
				else
				{
					// 正常回复客户端消息
					sinputLine = "["+ clientIP + "] : " + inputLine;
					out.println(sinputLine); // 发送回复
					out.flush();            // 刷新输出流
					System.out.println("服务器: " + sinputLine);
				}

				// 如果双方都发送了结束消息，则退出循环
				if( sinbye == true && inbye == true )
					break;

				// 如果客户端没有结束，继续读取客户端消息
				if( inbye == false )
				{
					inputLine = in.readLine();
					System.out.println( "["+ clientIP + "] : " + inputLine );
				}
			}while(true);

			// 关闭所有流和套接字
			out.close();
			in.close();
			socket.close();
			System.out.println("客户端" + clientNumber + "连接已关闭");

		} catch (IOException e) {
			e.printStackTrace();
		}
    }
}
