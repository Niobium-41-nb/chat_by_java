import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 增强版聊天服务器
 * 支持文件传输和群聊功能
 */
public class EnhancedChatServer {
    private static final int PORT = 8898;
    private static final int FILE_PORT = 8899;
    private ServerSocket serverSocket;
    private ServerSocket fileServerSocket;
    private volatile boolean listening = true;
    private static int clientNumber = 0;
    
    // 存储所有客户端连接
    private static Map<Integer, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<String, Group> groups = new ConcurrentHashMap<>();
    private static Map<Integer, String> clientUsernames = new ConcurrentHashMap<>();
    
    public static void main(String[] args) {
        new EnhancedChatServer().startServer();
    }
    
    public void startServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            fileServerSocket = new ServerSocket(FILE_PORT);
            System.out.println("增强版聊天服务器已启动，正在监听端口 " + PORT);
            System.out.println("文件传输服务已启动，正在监听端口 " + FILE_PORT);
            
            // 启动文件传输服务线程
            new Thread(this::startFileTransferService).start();
            
            // 创建默认群组
            groups.put("public", new Group("public", "公共聊天室"));
            groups.put("tech", new Group("tech", "技术交流"));
            groups.put("game", new Group("game", "游戏天地"));
            
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
    
    private void startFileTransferService() {
        try {
            while (listening) {
                Socket fileSocket = fileServerSocket.accept();
                new Thread(new FileTransferHandler(fileSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("文件传输服务错误: " + e.getMessage());
        }
    }
    
    public void stopServer() {
        listening = false;
        try {
            if (serverSocket != null) serverSocket.close();
            if (fileServerSocket != null) fileServerSocket.close();
            
            for (ClientHandler client : clients.values()) {
                client.closeConnection();
            }
            System.out.println("服务器已关闭");
        } catch (IOException e) {
            System.err.println("关闭服务器时出错: " + e.getMessage());
        }
    }
    
    // 静态方法供客户端处理器调用
    public static void sendMessageToClient(int targetId, String message, int senderId) {
        ClientHandler targetClient = clients.get(targetId);
        if (targetClient != null && targetClient.isConnected()) {
            String senderName = clientUsernames.getOrDefault(senderId, "用户" + senderId);
            targetClient.sendMessage("PRIVATE:" + senderName + ":" + message);
        }
    }
    
    public static void broadcastMessage(String message, int senderId) {
        String senderName = clientUsernames.getOrDefault(senderId, "用户" + senderId);
        for (Map.Entry<Integer, ClientHandler> entry : clients.entrySet()) {
            if (entry.getKey() != senderId && entry.getValue().isConnected()) {
                entry.getValue().sendMessage("PUBLIC:" + senderName + ":" + message);
            }
        }
    }
    
    public static void sendToGroup(String groupName, String message, int senderId) {
        Group group = groups.get(groupName);
        if (group != null) {
            String senderName = clientUsernames.getOrDefault(senderId, "用户" + senderId);
            group.broadcastMessage(senderName + ":" + message, senderId);
        }
    }
    
    public static void addToGroup(String groupName, int clientId) {
        Group group = groups.get(groupName);
        ClientHandler client = clients.get(clientId);
        if (group != null && client != null) {
            group.addMember(clientId, client);
            client.sendMessage("GROUP_JOIN:" + groupName + ":" + group.getDisplayName());
        }
    }
    
    public static void removeFromGroup(String groupName, int clientId) {
        Group group = groups.get(groupName);
        if (group != null) {
            group.removeMember(clientId);
            ClientHandler client = clients.get(clientId);
            if (client != null) {
                client.sendMessage("GROUP_LEAVE:" + groupName);
            }
        }
    }
    
    public static void removeClient(int clientId) {
        // 从所有群组中移除
        for (Group group : groups.values()) {
            group.removeMember(clientId);
        }
        clients.remove(clientId);
        clientUsernames.remove(clientId);
        System.out.println("客户端" + clientId + "已断开连接");
        broadcastMessage("离开了聊天室", clientId);
    }
    
    public static String getOnlineUsers() {
        StringBuilder sb = new StringBuilder("ONLINE_USERS:");
        for (Integer id : clients.keySet()) {
            if (clients.get(id).isConnected()) {
                String username = clientUsernames.getOrDefault(id, "用户" + id);
                sb.append(username).append("(").append(id).append(") ");
            }
        }
        return sb.toString();
    }
    
    public static String getGroupList() {
        StringBuilder sb = new StringBuilder("GROUP_LIST:");
        for (Group group : groups.values()) {
            sb.append(group.getName()).append(":").append(group.getDisplayName())
              .append(":").append(group.getMemberCount()).append(";");
        }
        return sb.toString();
    }
    
    public static String getGroupMembers(String groupName) {
        Group group = groups.get(groupName);
        if (group != null) {
            return "GROUP_MEMBERS:" + groupName + ":" + group.getMemberList();
        }
        return "GROUP_MEMBERS:" + groupName + ":";
    }
    
    public static void setClientUsername(int clientId, String username) {
        clientUsernames.put(clientId, username);
    }
    
    public static int findClientIdByUsername(String username) {
        for (Map.Entry<Integer, String> entry : clientUsernames.entrySet()) {
            if (entry.getValue().equals(username)) {
                return entry.getKey();
            }
        }
        return -1;
    }
    
    public static String getClientUsername(int clientId) {
        return clientUsernames.getOrDefault(clientId, "用户" + clientId);
    }
    
    public static ClientHandler getClient(int clientId) {
        return clients.get(clientId);
    }
}

/**
 * 群组类
 */
class Group {
    private String name;
    private String displayName;
    private Map<Integer, ClientHandler> members;
    
    public Group(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
        this.members = new ConcurrentHashMap<>();
    }
    
    public void addMember(int clientId, ClientHandler client) {
        members.put(clientId, client);
        broadcastSystemMessage("用户 " + EnhancedChatServer.getClientUsername(clientId) + " 加入了群组");
    }
    
    public void removeMember(int clientId) {
        if (members.remove(clientId) != null) {
            broadcastSystemMessage("用户 " + EnhancedChatServer.getClientUsername(clientId) + " 离开了群组");
        }
    }
    
    public void broadcastMessage(String message, int senderId) {
        String formattedMessage = "GROUP:" + name + ":" + message;
        for (Map.Entry<Integer, ClientHandler> entry : members.entrySet()) {
            if (entry.getKey() != senderId) {
                entry.getValue().sendMessage(formattedMessage);
            }
        }
    }
    
    public void broadcastSystemMessage(String message) {
        String formattedMessage = "GROUP_SYSTEM:" + name + ":" + message;
        for (ClientHandler member : members.values()) {
            member.sendMessage(formattedMessage);
        }
    }
    
    public String getMemberList() {
        StringBuilder sb = new StringBuilder();
        for (Integer id : members.keySet()) {
            String username = EnhancedChatServer.getClientUsername(id);
            sb.append(username).append("(").append(id).append(") ");
        }
        return sb.toString();
    }
    
    public int getMemberCount() {
        return members.size();
    }
    
    // Getters
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
}

/**
 * 文件传输处理器
 */
class FileTransferHandler implements Runnable {
    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;
    
    public FileTransferHandler(Socket socket) {
        this.socket = socket;
        try {
            this.dis = new DataInputStream(socket.getInputStream());
            this.dos = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("初始化文件传输处理器失败: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            // 读取文件传输请求类型
            String requestType = dis.readUTF();
            
            if ("SEND_FILE".equals(requestType)) {
                handleFileReceive();
            } else if ("REQUEST_FILE".equals(requestType)) {
                handleFileSend();
            }
            
        } catch (IOException e) {
            System.err.println("文件传输错误: " + e.getMessage());
        } finally {
            try {
                if (dis != null) dis.close();
                if (dos != null) dos.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("关闭文件传输连接时出错: " + e.getMessage());
            }
        }
    }
    
    private void handleFileReceive() throws IOException {
        // 读取文件信息
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        int senderId = dis.readInt();
        int receiverId = dis.readInt();
        String receiverType = dis.readUTF(); // "USER" or "GROUP"
        
        System.out.println("接收文件: " + fileName + " (" + fileSize + " bytes) from " + senderId + " to " + receiverId);
        
        // 通知接收方准备接收文件
        ClientHandler receiver = EnhancedChatServer.getClient(receiverId);
        if (receiver != null) {
            String senderName = EnhancedChatServer.getClientUsername(senderId);
            receiver.sendMessage("FILE_REQUEST:" + senderName + ":" + fileName + ":" + fileSize + ":" + socket.getInetAddress().getHostAddress());
        }
        
        // 确认接收
        dos.writeUTF("READY");
        dos.flush();
        
        // 接收文件数据
        File downloadsDir = new File("server_downloads");
        if (!downloadsDir.exists()) downloadsDir.mkdirs();
        
        File file = new File(downloadsDir, fileName);
        FileOutputStream fos = new FileOutputStream(file);
        
        byte[] buffer = new byte[4096];
        long totalRead = 0;
        int read;
        
        while (totalRead < fileSize && (read = dis.read(buffer)) > 0) {
            fos.write(buffer, 0, read);
            totalRead += read;
        }
        
        fos.close();
        System.out.println("文件接收完成: " + fileName);
        
        // 通知发送方传输完成
        dos.writeUTF("SUCCESS");
        dos.flush();
    }
    
    private void handleFileSend() throws IOException {
        String fileName = dis.readUTF();
        File file = new File("server_downloads", fileName);
        
        if (file.exists()) {
            dos.writeLong(file.length());
            dos.writeUTF("READY");
            dos.flush();
            
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            
            while ((read = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, read);
            }
            
            fis.close();
            System.out.println("文件发送完成: " + fileName);
        } else {
            dos.writeLong(0);
            dos.writeUTF("FILE_NOT_FOUND");
            dos.flush();
        }
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
            // 读取用户名
            String username = in.readLine();
            EnhancedChatServer.setClientUsername(clientId, username);
            
            // 发送欢迎消息
            out.println("WELCOME:你的用户ID是: " + clientId);
            out.println(EnhancedChatServer.getOnlineUsers());
            out.println(EnhancedChatServer.getGroupList());
            
            // 加入默认公共群组
            EnhancedChatServer.addToGroup("public", clientId);
            
            // 广播新用户加入
            EnhancedChatServer.broadcastMessage("加入了聊天室", clientId);
            
            String inputLine;
            while ((inputLine = in.readLine()) != null && connected) {
                System.out.println("客户端" + clientId + "(" + username + "): " + inputLine);
                
                if (inputLine.startsWith("/")) {
                    handleCommand(inputLine);
                } else if (inputLine.startsWith("@")) {
                    handlePrivateMessage(inputLine);
                } else if (inputLine.startsWith("#")) {
                    handleGroupMessage(inputLine);
                } else {
                    EnhancedChatServer.broadcastMessage(inputLine, clientId);
                }
                
                if (inputLine.equals("Bye.")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("客户端" + clientId + "连接错误: " + e.getMessage());
        } finally {
            closeConnection();
            EnhancedChatServer.removeClient(clientId);
        }
    }
    
    private void handleCommand(String command) {
        String[] parts = command.substring(1).split(" ", 2);
        String cmd = parts[0];
        
        switch (cmd) {
            case "users":
                out.println(EnhancedChatServer.getOnlineUsers());
                break;
                
            case "groups":
                out.println(EnhancedChatServer.getGroupList());
                break;
                
            case "join":
                if (parts.length > 1) {
                    EnhancedChatServer.addToGroup(parts[1], clientId);
                }
                break;
                
            case "leave":
                if (parts.length > 1) {
                    EnhancedChatServer.removeFromGroup(parts[1], clientId);
                }
                break;
                
            case "members":
                if (parts.length > 1) {
                    out.println(EnhancedChatServer.getGroupMembers(parts[1]));
                }
                break;
                
            case "file":
                if (parts.length > 1) {
                    handleFileCommand(parts[1]);
                }
                break;
                
            case "help":
                out.println("可用命令:");
                out.println("/users - 查看在线用户");
                out.println("/groups - 查看群组列表");
                out.println("/join [群组名] - 加入群组");
                out.println("/leave [群组名] - 离开群组");
                out.println("/members [群组名] - 查看群组成员");
                out.println("/file send [用户名] [文件路径] - 发送文件");
                out.println("@用户名 消息 - 私聊");
                out.println("#群组名 消息 - 群聊");
                break;
                
            default:
                out.println("未知命令: " + command);
        }
    }
    
    private void handlePrivateMessage(String message) {
        try {
            String[] parts = message.substring(1).split(" ", 2);
            if (parts.length >= 2) {
                String targetUsername = parts[0];
                String privateMsg = parts[1];
                int targetId = EnhancedChatServer.findClientIdByUsername(targetUsername);
                
                if (targetId != -1) {
                    EnhancedChatServer.sendMessageToClient(targetId, privateMsg, clientId);
                    out.println("你对" + targetUsername + "说: " + privateMsg);
                } else {
                    out.println("用户 " + targetUsername + " 不在线");
                }
            }
        } catch (Exception e) {
            out.println("私聊格式错误，正确格式: @用户名 消息内容");
        }
    }
    
    private void handleGroupMessage(String message) {
        try {
            String[] parts = message.substring(1).split(" ", 2);
            if (parts.length >= 2) {
                String groupName = parts[0];
                String groupMsg = parts[1];
                EnhancedChatServer.sendToGroup(groupName, groupMsg, clientId);
                out.println("你在[" + groupName + "]说: " + groupMsg);
            }
        } catch (Exception e) {
            out.println("群聊格式错误，正确格式: #群组名 消息内容");
        }
    }
    
    private void handleFileCommand(String fileCmd) {
        try {
            String[] parts = fileCmd.split(" ");
            if (parts.length >= 3 && "send".equals(parts[0])) {
                String targetUsername = parts[1];
                String filePath = parts[2];
                sendFileToUser(targetUsername, filePath);
            }
        } catch (Exception e) {
            out.println("文件命令格式错误: /file send 用户名 文件路径");
        }
    }
    
    private void sendFileToUser(String targetUsername, String filePath) {
        try {
            int targetId = EnhancedChatServer.findClientIdByUsername(targetUsername);
            if (targetId == -1) {
                out.println("用户 " + targetUsername + " 不在线");
                return;
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                out.println("文件不存在: " + filePath);
                return;
            }
            
            // 连接到文件传输服务器
            Socket fileSocket = new Socket("localhost", 8899);
            DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(fileSocket.getInputStream());
            
            // 发送文件传输请求
            dos.writeUTF("SEND_FILE");
            dos.writeUTF(file.getName());
            dos.writeLong(file.length());
            dos.writeInt(clientId);
            dos.writeInt(targetId);
            dos.writeUTF("USER");
            dos.flush();
            
            // 等待接收方准备
            String response = dis.readUTF();
            if ("READY".equals(response)) {
                // 发送文件数据
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int read;
                
                while ((read = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, read);
                }
                
                fis.close();
                
                // 等待传输完成确认
                String result = dis.readUTF();
                if ("SUCCESS".equals(result)) {
                    out.println("文件发送成功: " + file.getName());
                } else {
                    out.println("文件发送失败");
                }
            }
            
            fileSocket.close();
            
        } catch (IOException e) {
            out.println("文件发送错误: " + e.getMessage());
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