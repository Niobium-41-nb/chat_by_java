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
        while (true) {
            // 如果服务器没有结束，读取所有可用的服务器消息
            if (!sbye) {
                while (in.ready()) { // 循环读取所有可用的消息
                    fromServer = in.readLine();
                    if (fromServer == null) {
                        sbye = true;
                        break;
                    }
                    System.out.println("来自服务器: " + fromServer);
                    
                    if (fromServer.equals("Bye.")) {
                        sbye = true;
                    }
                }
            }

            // 如果用户没有结束，检查是否有输入
            if (!ubye) {
                if (stdIn.ready()) { // 检查标准输入是否有数据可读
                    System.out.print("客户端输入:");
                    fromUser = stdIn.readLine();
                    
                    out.println(fromUser);
                    out.flush();
                    
                    if (fromUser.equals("Bye.")) {
                        ubye = true;
                    }
                }
            }

            // 短暂休眠，避免CPU过度占用
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 如果双方都发送了结束消息，退出循环
            if (ubye && sbye) {
                break;
            }
            
            // 如果服务器单方面断开连接，也退出循环
            if (sbye && !ubye) {
                System.out.println("服务器已断开连接");
                break;
            }
        }

		// 关闭所有流和套接字
		out.close();
		in.close();
		stdIn.close();
		client.close();
		System.out.println("客户端已断开连接");
    }
}