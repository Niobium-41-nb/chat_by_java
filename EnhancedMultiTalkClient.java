import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * 增强的多线程聊天客户端
 * 支持并行通信、私聊、群聊等功能
 */
public class EnhancedMultiTalkClient {
    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8898;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private boolean connected = false;
    
    public static void main(String[] args) {
        EnhancedMultiTalkClient client = new EnhancedMultiTalkClient();
        client.start();
    }
    
    public void start() {
        scanner = new Scanner(System.in);
        
        try {
            // 连接服务器
            connectToServer();
            
            // 启动消息接收线程
            Thread receiverThread = new Thread(this::receiveMessages);
            receiverThread.setDaemon(true);
            receiverThread.start();
            
            // 处理用户输入
            handleUserInput();
            
        } catch (IOException e) {
            System.err.println("客户端错误: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    /**
     * 连接到服务器
     */
    private void connectToServer() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        connected = true;
        
        System.out.println("已连接到服务器 " + SERVER_HOST + ":" + SERVER_PORT);
        
        // 读取服务器欢迎消息
        String serverMessage;
        while ((serverMessage = in.readLine()) != null) {
            System.out.println(serverMessage);
            if (serverMessage.contains("认证成功")) {
                break;
            }
        }
    }
    
    /**
     * 接收服务器消息
     */
    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println(message);
                
                // 如果收到再见消息，断开连接
                if (message.contains("再见")) {
                    connected = false;
                    break;
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("连接断开: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理用户输入
     */
    private void handleUserInput() {
        System.out.println("\n=== Java QQ 客户端 ===");
        System.out.println("输入消息开始聊天，输入 'help' 查看帮助");
        
        while (connected && scanner.hasNextLine()) {
            String userInput = scanner.nextLine().trim();
            
            if (userInput.isEmpty()) {
                continue;
            }
            
            if (userInput.equalsIgnoreCase("bye")) {
                out.println("bye");
                break;
            } else if (userInput.equalsIgnoreCase("help")) {
                showHelp();
            } else if (userInput.equalsIgnoreCase("list")) {
                out.println("list");
            } else {
                // 发送消息到服务器
                out.println(userInput);
            }
        }
    }
    
    /**
     * 显示帮助信息
     */
    private void showHelp() {
        System.out.println("\n=== 命令帮助 ===");
        System.out.println("@用户名 消息内容 - 私聊指定用户");
        System.out.println("all 消息内容 - 发送群聊消息");
        System.out.println("list - 查看在线用户");
        System.out.println("help - 显示此帮助");
        System.out.println("bye - 退出程序");
        System.out.println("================\n");
    }
    
    /**
     * 断开连接
     */
    private void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (scanner != null) scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("客户端已断开连接");
    }
}