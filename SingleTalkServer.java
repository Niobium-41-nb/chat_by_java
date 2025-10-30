import java.io.*;
import java.net.*;

/**
 * 单线程聊天服务器类
 * 支持单个客户端连接进行聊天通信
 */
public class SingleTalkServer
{
    /**
     * 服务器主方法
     * @param args 命令行参数
     * @throws IOException 输入输出异常
     */
    public static void main(String[] args) throws IOException
	{

        ServerSocket serverSocket = null;  // 服务器套接字
        // 创建服务器套接字，监听8898端口
        try {
            serverSocket = new ServerSocket(8898);
            System.out.println("单线程聊天服务器已启动，正在监听端口8898...");
        } catch (IOException e) {
            System.err.println("无法在端口8898上监听。");
            System.exit(1);
        }

        Socket clientSocket = null;  // 客户端套接字
        // 等待客户端连接
        try {
            clientSocket = serverSocket.accept();  // 在此等待客户端的连接
            System.out.println("接受客户端连接成功！");
        } catch (IOException e) {
            System.err.println("接受连接失败。");
            System.exit(1);
        }

        // 创建输入/输出流
	PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);  // 自动刷新输出流
        BufferedReader in = new BufferedReader(
				new InputStreamReader(
				clientSocket.getInputStream()));

	// 从标准输入流（控制台）获取服务器输入信息
        BufferedReader sin = new BufferedReader( new InputStreamReader( System.in ) );

	boolean  sinbye = false;  // 服务器结束标志
	boolean  inbye = false;   // 客户端结束标志
	String sinputLine, inputLine;  // 服务器输入和客户端输入
        
        // 读取客户端的第一条消息
        inputLine = in.readLine();
	System.out.println( "来自客户端: " + inputLine );

	// 获取服务器的第一条输入
	System.out.print("服务器输入:");
	sinputLine = sin.readLine();

        // 通信循环
        while( true )
	{
		// 如果服务器没有结束，发送消息给客户端
		if( sinbye == false )
		{
			out.println(sinputLine);  // 发送消息给客户端
			out.flush();              // 刷新输出流
			//System.out.println("服务器: " + sinputLine);  // 可选：显示发送的消息

			// 如果服务器输入"Bye."，设置服务器结束标志
			if (sinputLine.equals("Bye."))
				sinbye = true;
		}

		// 如果客户端没有结束，读取客户端消息
		if( inbye == false )
		{
			inputLine = in.readLine();  // 读取客户端消息
			System.out.println( "来自客户端: " + inputLine );  // 显示客户端消息

			// 如果客户端发送"Bye."，设置客户端结束标志
			if (inputLine.equals("Bye."))
				inbye = true;
		}

		// 如果服务器没有结束，继续获取服务器输入
		if( sinbye == false )
		{
			System.out.print("服务器输入:");
			sinputLine = sin.readLine();  // 读取服务器输入
		}

		// 如果双方都发送了结束消息，退出循环
		if( sinbye == true && inbye == true )
			break;
        }

        // 关闭所有流和套接字
        out.close();
        in.close();
	sin.close();

        clientSocket.close();
        serverSocket.close();
        System.out.println("服务器已关闭");
    }
}
