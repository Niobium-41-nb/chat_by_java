import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 增强的多线程聊天服务器
 * 支持多个客户端连接、客户端间单独通信、并行通信
 */
public class EnhancedMultiTalkServer {
    private static final int PORT = 8898;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<String, String> userCredentials = new ConcurrentHashMap<>();
    
    static {
        // 初始化一些测试用户
        userCredentials.put("user1", "pass1");
        userCredentials.put("user2", "pass2");
        userCredentials.put("user3", "pass3");
    }
    
    public static void main(String[] args) {
        ServerSocket serverSocket = null;
        
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("增强版QQ服务器已启动，正在监听端口 " + PORT + "...");
            System.out.println("可用测试用户：");
            for (String username : userCredentials.keySet()) {
                System.out.println("用户名: " + username + " 密码: " + userCredentials.get(username));
            }
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("新客户端连接: " + clientSocket.getInetAddress());
                
                // 为每个客户端创建新的处理线程
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 客户端处理线程
     */
    static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private boolean authenticated = false;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // 认证流程
                if (!authenticate()) {
                    return;
                }
                
                // 发送欢迎消息和在线用户列表
                sendWelcomeMessage();
                
                // 处理客户端消息
                handleClientMessages();
                
            } catch (IOException e) {
                System.err.println("客户端处理错误: " + e.getMessage());
            } finally {
                disconnect();
            }
        }
        
        /**
         * 用户认证
         */
        private boolean authenticate() throws IOException {
            out.println("请输入用户名:");
            String inputUsername = in.readLine();
            out.println("请输入密码:");
            String inputPassword = in.readLine();
            
            if (userCredentials.containsKey(inputUsername) && 
                userCredentials.get(inputUsername).equals(inputPassword)) {
                this.username = inputUsername;
                authenticated = true;
                clients.put(username, this);
                out.println("认证成功！欢迎 " + username);
                broadcastSystemMessage(username + " 上线了");
                return true;
            } else {
                out.println("认证失败！用户名或密码错误");
                return false;
            }
        }
        
        /**
         * 发送欢迎消息和在线用户列表
         */
        private void sendWelcomeMessage() {
            out.println("=== 欢迎使用Java QQ ===");
            out.println("在线用户列表:");
            for (String user : clients.keySet()) {
                if (!user.equals(username)) {
                    out.println("- " + user);
                }
            }
            out.println("=== 命令说明 ===");
            out.println("@用户名 消息内容 - 私聊");
            out.println("all 消息内容 - 群聊");
            out.println("list - 查看在线用户");
            out.println("bye - 退出");
            out.println("=========================");
        }
        
        /**
         * 处理客户端消息
         */
        private void handleClientMessages() throws IOException {
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("bye")) {
                    out.println("再见！");
                    break;
                } else if (message.equalsIgnoreCase("list")) {
                    sendOnlineUsers();
                } else if (message.startsWith("@")) {
                    // 私聊消息
                    handlePrivateMessage(message);
                } else if (message.startsWith("all ")) {
                    // 群聊消息
                    handleGroupMessage(message);
                } else {
                    out.println("未知命令，请输入 'list' 查看可用命令");
                }
            }
        }
        
        /**
         * 处理私聊消息
         */
        private void handlePrivateMessage(String message) {
            int spaceIndex = message.indexOf(" ");
            if (spaceIndex == -1) {
                out.println("私聊格式错误，请使用: @用户名 消息内容");
                return;
            }
            
            String targetUser = message.substring(1, spaceIndex);
            String content = message.substring(spaceIndex + 1);
            
            ClientHandler target = clients.get(targetUser);
            if (target != null) {
                target.sendPrivateMessage(username, content);
                out.println("发送给 " + targetUser + ": " + content);
            } else {
                out.println("用户 " + targetUser + " 不在线或不存在");
            }
        }
        
        /**
         * 处理群聊消息
         */
        private void handleGroupMessage(String message) {
            String content = message.substring(4);
            broadcastMessage(username, content);
            out.println("群聊消息已发送: " + content);
        }
        
        /**
         * 发送私聊消息
         */
        public void sendPrivateMessage(String fromUser, String message) {
            out.println("[私聊] " + fromUser + ": " + message);
        }
        
        /**
         * 发送在线用户列表
         */
        private void sendOnlineUsers() {
            out.println("=== 在线用户 ===");
            for (String user : clients.keySet()) {
                if (!user.equals(username)) {
                    out.println("- " + user);
                }
            }
            out.println("================");
        }
        
        /**
         * 广播系统消息
         */
        private void broadcastSystemMessage(String message) {
            for (ClientHandler client : clients.values()) {
                if (client != this) {
                    client.out.println("[系统] " + message);
                }
            }
        }
        
        /**
         * 广播群聊消息
         */
        private void broadcastMessage(String fromUser, String message) {
            for (ClientHandler client : clients.values()) {
                if (client != this) {
                    client.out.println("[群聊] " + fromUser + ": " + message);
                }
            }
        }
        
        /**
         * 断开连接
         */
        private void disconnect() {
            if (authenticated) {
                clients.remove(username);
                broadcastSystemMessage(username + " 下线了");
                System.out.println("用户 " + username + " 断开连接");
            }
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}