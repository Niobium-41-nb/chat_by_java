import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 增强版多线程聊天服务器
 * 支持客户端单独通信和并行通信
 */
public class EnhancedMultiTalkServer {
    private static final int PORT = 8898;
    private ServerSocket serverSocket;
    private volatile boolean listening = true;
    private static int clientNumber = 0;
    
    // 存储所有客户端连接
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<String, Integer> userMap = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        new EnhancedMultiTalkServer().startServer();
    }
    
    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("增强版聊天服务器已启动，正在监听端口 " + PORT + "...");
            
            while (listening) {
                Socket socket = serverSocket.accept();
                clientNumber++;
                ClientHandler clientHandler = new ClientHandler(socket, clientNumber);
                clients.put(clientNumber, clientHandler);
                new Thread(clientHandler).start();
            }
            
        } catch (IOException e) {
            System.err.println("服务器错误: " + e.getMessage());
        } finally {
            stopServer();
        }
    }
    
    public void stopServer() {
        listening = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            // 关闭所有客户端连接
            for (ClientHandler client : clients.values()) {
                client.closeConnection();
            }
            System.out.println("服务器已关闭");
        } catch (IOException e) {
            System.err.println("关闭服务器时出错: " + e.getMessage());
        }
    }
    
    // 向指定客户端发送消息
    public static void sendMessageToClient(int targetId, String message, int senderId) {
        ClientHandler targetClient = clients.get(targetId);
        if (targetClient != null && targetClient.isConnected()) {
            targetClient.sendMessage("用户" + senderId + "对你说: " + message);
        }
    }
    
    // 广播消息给所有客户端
    public static void broadcastMessage(String message, int senderId) {
        for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) {
            if (entry.getKey() != senderId && entry.getValue().isConnected()) {
                entry.getValue().sendMessage("用户" + senderId + ": " + message);
            }
        }
    }
    
    // 移除客户端
    public static void removeClient(int clientId) {
        clients.remove(clientId);
        System.out.println("客户端" + clientId + "已断开连接");
        broadcastMessage("用户" + clientId + "离开了聊天室", -1);
    }
    
    // 获取在线用户列表
    public static String getOnlineUsers() {
        StringBuilder sb = new StringBuilder("在线用户: ");
        for (Integer id : clients.keySet()) {
            if (clients.get(id).isConnected()) {
                sb.append("用户").append(id).append(" ");
            }
        }
        return sb.toString();
    }
}

/**
 * 客户端处理线程
 */
class ClientHandler implements Runnable {
    private Socket socket;
    private int clientId;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean connected = true;
    
    public ClientHandler(Socket socket, int clientId) {
        this.socket = socket;
        this.clientId = clientId;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.err.println("初始化客户端处理器失败: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            // 发送欢迎消息和用户ID
            out.println("欢迎连接到聊天服务器! 你的用户ID是: " + clientId);
            out.println(EnhancedMultiTalkServer.getOnlineUsers());
            
            // 广播新用户加入
            EnhancedMultiTalkServer.broadcastMessage("加入了聊天室", clientId);
            
            String inputLine;
            while ((inputLine = in.readLine()) != null && connected) {
                System.out.println("客户端" + clientId + ": " + inputLine);
                
                // 处理特殊命令
                if (inputLine.startsWith("/")) {
                    handleCommand(inputLine);
                } else if (inputLine.startsWith("@")) {
                    // 私聊格式: @用户ID 消息内容
                    handlePrivateMessage(inputLine);
                } else {
                    // 广播消息
                    EnhancedMultiTalkServer.broadcastMessage(inputLine, clientId);
                }
                
                if (inputLine.equals("Bye.")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("客户端" + clientId + "连接错误: " + e.getMessage());
        } finally {
            closeConnection();
            EnhancedMultiTalkServer.removeClient(clientId);
        }
    }
    
    private void handleCommand(String command) {
        if (command.equals("/users")) {
            out.println(EnhancedMultiTalkServer.getOnlineUsers());
        } else if (command.equals("/help")) {
            out.println("可用命令: /users - 查看在线用户, /help - 帮助信息");
            out.println("私聊格式: @用户ID 消息内容");
        } else {
            out.println("未知命令: " + command);
        }
    }
    
    private void handlePrivateMessage(String message) {
        try {
            // 解析格式: @用户ID 消息内容
            String[] parts = message.substring(1).split(" ", 2);
            if (parts.length >= 2) {
                int targetId = Integer.parseInt(parts[0]);
                String privateMsg = parts[1];
                EnhancedMultiTalkServer.sendMessageToClient(targetId, privateMsg, clientId);
                out.println("你对用户" + targetId + "说: " + privateMsg);
            } else {
                out.println("私聊格式错误，正确格式: @用户ID 消息内容");
            }
        } catch (NumberFormatException e) {
            out.println("用户ID格式错误");
        }
    }
    
    public void sendMessage(String message) {
        if (connected && out != null) {
            out.println(message);
        }
    }
    
    public void closeConnection() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("关闭连接时出错: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return connected && !socket.isClosed();
    }
    
    public int getClientId() {
        return clientId;
    }
}