import java.io.*;
import java.net.*;

/**
 * 单线程聊天客户端类
 * 连接到服务器进行聊天通信
 */
public class SingleTalkClient
{
    /**
     * 客户端主方法
     * @param args 命令行参数
     * @throws IOException 输入输出异常
     */
    public static void main(String[] args) throws IOException
	{

        Socket client = null;        // 客户端套接字
        PrintWriter out = null;      // 输出流
        BufferedReader in = null;    // 输入流

        // 连接到服务器
        try {
            client = new Socket("127.0.0.1", 8898);  // 连接到本地服务器的8898端口
            out = new PrintWriter(client.getOutputStream(), true); // 自动刷新输出流
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            System.out.println("已连接到服务器 127.0.0.1:8898");
        } catch (UnknownHostException e) {
            System.err.println("未知主机: 127.0.0.1.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("无法获取到 127.0.0.1 连接的I/O。");
            System.exit(1);
        }

        // 从标准输入流（控制台）获取客户端输入信息
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
			
		String fromServer, fromUser;  // 服务器消息和用户输入
		boolean sbye = false;         // 服务器结束标志
		boolean ubye = false;         // 用户结束标志

		// 获取用户的第一条输入
		System.out.print("客户端输入:");
		fromUser = stdIn.readLine();
			
		// 通信循环
		while( true ){
			// 如果用户没有结束，发送消息给服务器
			if( ubye == false )
			{
				out.println(fromUser);  // 发送用户消息
				out.flush();            // 刷新输出流
				//System.out.println("客户端: " + fromUser);  // 可选：显示发送的消息
				
				// 如果用户输入"Bye."，设置用户结束标志
				if (fromUser.equals("Bye."))
					ubye = true;
			}

			// 如果服务器没有结束，读取服务器消息
			if( sbye == false )
			{
				fromServer = in.readLine();  // 读取服务器消息
				System.out.println("来自服务器: " + fromServer);  // 显示服务器消息
				
				// 如果服务器发送"Bye."，设置服务器结束标志
				if (fromServer.equals("Bye."))
					sbye = true;
			}

			// 如果用户没有结束，继续获取用户输入
			if( ubye == false )
			{
				System.out.print("客户端输入:");
				fromUser = stdIn.readLine();  // 读取用户输入
			}

			// 如果双方都发送了结束消息，退出循环
			if( ubye == true && sbye == true )
				break;
		}

		// 关闭所有流和套接字
		out.close();
		in.close();
		stdIn.close();
		client.close();
		System.out.println("客户端已断开连接");
    }
}
