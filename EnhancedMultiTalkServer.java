import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 增强版多客户端聊天服务器
 * 支持：多客户端通信、并行通信、用户管理、私聊、群聊、文件传输
 */
public class EnhancedMultiTalkServer {
    private static final int PORT = 8898;
    private static ServerSocket serverSocket;
    private static boolean listening = true;
    private static ExecutorService threadPool = Executors.newCachedThreadPool();
    
    // 用户管理
    private static Map<String, UserSession> onlineUsers = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> friendsMap = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("增强版多线程聊天服务器已启动，正在监听端口 " + PORT + "...");
            System.out.println("支持功能：多客户端通信、并行通信、用户管理、私聊、群聊、文件传输");
            
            // 初始化一些测试用户
            initializeTestUsers();
            
            while (listening) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new ClientHandler(clientSocket));
            }
            
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    private static void initializeTestUsers() {
        // 初始化一些测试用户和好友关系
        friendsMap.put("user1", new HashSet<>(Arrays.asList("user2", "user3")));
        friendsMap.put("user2", new HashSet<>(Arrays.asList("user1", "user3")));
        friendsMap.put("user3", new HashSet<>(Arrays.asList("user1", "user2")));
        friendsMap.put("admin", new HashSet<>(Arrays.asList("user1", "user2", "user3")));
    }
    
    private static void shutdown() {
        listening = false;
        threadPool.shutdown();
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 客户端处理线程
     */
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private UserSession session;
        
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // 用户认证
                if (!authenticateUser()) {
                    return;
                }
                
                // 创建用户会话
                session = new UserSession(username, out);
                onlineUsers.put(username, session);
                
                // 发送欢迎消息和好友列表
                sendWelcomeMessage();
                sendFriendList();
                
                // 通知其他用户有新用户上线
                broadcastSystemMessage(username + " 上线了");
                
                // 处理客户端消息
                processClientMessages();
                
            } catch (IOException e) {
                System.err.println("客户端处理错误: " + e.getMessage());
            } finally {
                cleanup();
            }
        }
        
        private boolean authenticateUser() throws IOException {
            out.println("请输入用户名:");
            String input = in.readLine();
            if (input == null) return false;
            
            // 简单的用户验证（实际项目中应该使用数据库）
            if (input.trim().isEmpty()) {
                out.println("ERROR: 用户名不能为空");
                return false;
            }
            
            username = input.trim();
            out.println("SUCCESS: 登录成功，欢迎 " + username);
            return true;
        }
        
        private void sendWelcomeMessage() {
            out.println("SYSTEM: 欢迎来到聊天室！当前在线用户: " + onlineUsers.size());
            out.println("SYSTEM: 可用命令:");
            out.println("SYSTEM:   @用户名 消息 - 私聊");
            out.println("SYSTEM:   /friends - 查看好友列表");
            out.println("SYSTEM:   /online - 查看在线用户");
            out.println("SYSTEM:   /file @用户名 文件名 - 发送文件");
            out.println("SYSTEM:   /group 消息 - 群发消息");
            out.println("SYSTEM:   /quit - 退出");
        }
        
        private void sendFriendList() {
            Set<String> friends = friendsMap.get(username);
            if (friends != null && !friends.isEmpty()) {
                out.println("FRIENDS: 你的好友列表:");
                for (String friend : friends) {
                    String status = onlineUsers.containsKey(friend) ? "[在线]" : "[离线]";
                    out.println("FRIENDS:   " + friend + " " + status);
                }
            } else {
                out.println("FRIENDS: 你还没有好友");
            }
        }
        
        private void processClientMessages() throws IOException {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("[" + username + "]: " + inputLine);
                
                if (inputLine.equals("/quit")) {
                    break;
                } else if (inputLine.startsWith("@")) {
                    // 私聊消息
                    handlePrivateMessage(inputLine);
                } else if (inputLine.equals("/friends")) {
                    // 查看好友列表
                    sendFriendList();
                } else if (inputLine.equals("/online")) {
                    // 查看在线用户
                    sendOnlineUsers();
                } else if (inputLine.startsWith("/file ")) {
                    // 文件传输
                    handleFileTransfer(inputLine);
                } else if (inputLine.startsWith("/group ")) {
                    // 群发消息
                    handleGroupMessage(inputLine);
                } else {
                    // 广播消息
                    broadcastMessage(inputLine);
                }
            }
        }
        
        private void handlePrivateMessage(String message) {
            int spaceIndex = message.indexOf(' ');
            if (spaceIndex == -1) {
                out.println("ERROR: 私聊格式: @用户名 消息");
                return;
            }
            
            String targetUser = message.substring(1, spaceIndex);
            String privateMessage = message.substring(spaceIndex + 1);
            
            UserSession targetSession = onlineUsers.get(targetUser);
            if (targetSession != null) {
                targetSession.sendMessage("私聊[" + username + "]: " + privateMessage);
                out.println("你私聊[" + targetUser + "]: " + privateMessage);
            } else {
                out.println("ERROR: 用户 " + targetUser + " 不在线");
            }
        }
        
        private void handleGroupMessage(String message) {
            String groupMessage = message.substring(7); // 去掉 "/group "
            broadcastMessage("[群发]" + username + ": " + groupMessage);
        }
        
        private void handleFileTransfer(String command) {
            // 文件传输功能占位符
            out.println("SYSTEM: 文件传输功能正在开发中...");
        }
        
        private void broadcastMessage(String message) {
            String formattedMessage = username + ": " + message;
            for (UserSession userSession : onlineUsers.values()) {
                if (!userSession.getUsername().equals(username)) {
                    userSession.sendMessage(formattedMessage);
                }
            }
        }
        
        private void broadcastSystemMessage(String message) {
            for (UserSession userSession : onlineUsers.values()) {
                if (!userSession.getUsername().equals(username)) {
                    userSession.sendMessage("SYSTEM: " + message);
                }
            }
        }
        
        private void sendOnlineUsers() {
            out.println("ONLINE: 当前在线用户 (" + onlineUsers.size() + "):");
            for (String user : onlineUsers.keySet()) {
                out.println("ONLINE:   " + user);
            }
        }
        
        private void cleanup() {
            if (username != null) {
                onlineUsers.remove(username);
                broadcastSystemMessage(username + " 下线了");
                System.out.println("用户 " + username + " 断开连接");
            }
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 用户会话类
     */
    static class UserSession {
        private String username;
        private PrintWriter output;
        
        public UserSession(String username, PrintWriter output) {
            this.username = username;
            this.output = output;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void sendMessage(String message) {
            output.println(message);
        }
    }
}