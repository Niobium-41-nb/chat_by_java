import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 客户端管理器类 - 用于线程间通信
 */
class ClientManager {
    private static final List<PrintWriter> clients = new ArrayList<>();
    private static final Map<String, PrintWriter> Map_User = Collections.synchronizedMap(new HashMap<>());
    private static final Map<PrintWriter, String> to_User = Collections.synchronizedMap(new HashMap<>());
    private static int idx = 0;
    
    /**
     * 添加客户端输出流
     * @param out 客户端输出流
     */
    public static synchronized String addClient(PrintWriter out) {
        String username = "user" + idx;
        idx++;
        Map_User.put(username, out);
        to_User.put(out, username);
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
     * @param to_username 目标用户名
     */
    public static synchronized void sicast(String message, String to_username) {
        PrintWriter targetClient = Map_User.get(to_username);
        if (targetClient != null) {
            targetClient.println(message);
            targetClient.flush();
        }
    }
    
    /**
     * 私发文件
     * @param filename 文件名
     * @param fileData 文件数据
     * @param to_username 目标用户名
     * @param from_username 发送者用户名
     */
    public static synchronized void sendFile(String filename, String fileData, String to_username, String from_username) {
        PrintWriter targetClient = Map_User.get(to_username);
        if (targetClient != null) {
            String fileMessage = "FILE:" + from_username + ":" + filename + ":" + fileData;
            targetClient.println(fileMessage);
            targetClient.flush();
        }
    }
    
    /**
     * 广播消息
     * @param message 要广播的消息
     * @param excludeClient 要排除的客户端
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
    
    /**
     * 获取所有在线用户
     * @return 在线用户列表
     */
    public static synchronized List<String> getOnlineUsers() {
        return new ArrayList<>(Map_User.keySet());
    }
    
    /**
     * 根据用户名获取客户端输出流
     * @param username 用户名
     * @return 客户端输出流
     */
    public static synchronized PrintWriter getClientByUsername(String username) {
        return Map_User.get(username);
    }
}

/**
 * 多线程聊天服务器类
 * 支持多个客户端同时连接并进行聊天
 */
public class MultiTalkServer {
    /**
     * 服务器主方法
     * @param args 命令行参数
     * @throws IOException 输入输出异常
     */
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;        // 服务器套接字
        boolean listening = true;                // 服务器监听标志
        int clientNumber = 0;                   // 客户端计数器

        // 创建服务器套接字，监听8898端口
        try {
            serverSocket = new ServerSocket(8898);
            System.out.println("多线程聊天服务器已启动，正在监听端口8898...");
        } catch (IOException e) {
            System.err.println("无法在端口8898上监听。");
            System.exit(-1);
        }

        // 主循环：持续监听客户端连接
        while (listening) {
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
class MultiTalkServerThread extends Thread {
    private final Socket socket;        // 客户端套接字
    private final int clientNumber;     // 客户端编号
    private String username;            // 客户端用户名

    /**
     * 判断消息发给谁，提取$后面的人名
     * @param Message 输入的消息字符串
     * @return 提取到的用户名，如果没有找到则返回null
     */
    private static String to_who(String Message) {
        // 检查消息是否为空
        if (Message == null || Message.trim().isEmpty()) {
            return null;
        }
        
        // 查找$符号的位置
        int atIndex = Message.indexOf('$');
        
        // 如果没有找到$符号，返回null
        if (atIndex == -1) {
            return null;
        }
        
        // 从$符号后面开始查找用户名的结束位置（空格、标点符号或字符串末尾）
        int endIndex = atIndex + 1;
        while (endIndex < Message.length()) {
            char c = Message.charAt(endIndex);
            // 判断字符是否为字母、数字或下划线（用户名的合法字符）
            if (!Character.isLetterOrDigit(c) && c != '_') {
                break;
            }
            endIndex++;
        }
        
        // 如果$后面没有有效字符，返回null
        if (endIndex == atIndex + 1) {
            return null;
        }
        
        // 提取并返回$后面的用户名
        return Message.substring(atIndex + 1, endIndex);
    }
    
    /**
     * 构造函数
     * @param socket 客户端套接字
     * @param clientNumber 客户端编号
     */
    public MultiTalkServerThread(Socket socket, int clientNumber) {
        super("MultiTalkServerThread");  // 设置线程名称
        this.socket = socket;
        this.clientNumber = clientNumber;
        System.out.println("接受客户端" + clientNumber + "连接");
    }

    /**
     * 线程执行方法
     * 处理与客户端的通信
     */
    @Override
    public void run() {
        PrintWriter out = null;
        try {
            // 创建输出流，用于向客户端发送数据
            out = new PrintWriter(socket.getOutputStream(), true);  // 自动刷新
            // 创建输入流，用于接收客户端数据
            BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                        socket.getInputStream()));

            // 将客户端添加到管理器
            username = ClientManager.addClient(out);
            
            // 发送欢迎消息和在线用户列表
            out.println("WELCOME:" + username);
            sendOnlineUsers(out);
            
            // 通知所有客户端有新用户加入
            String joinMessage = "系统消息: 客户端" + username + "加入了聊天室，当前在线人数: " + ClientManager.getClientCount();
            System.out.println(joinMessage);
            ClientManager.broadcast(joinMessage, out);
            broadcastOnlineUsers();

            String inputLine;
            boolean inbye = false;         // 客户端结束标志

            // 通信循环
            do {
                // 读取客户端信息
                inputLine = in.readLine();
                
                // 检查连接是否断开
                if (inputLine == null) {
                    System.out.println("客户端" + username + "断开连接");
                    break;
                }
                
                System.out.println("来自客户端" + username + ": " + inputLine);
                
                // 如果客户端发送"Bye."，则结束对话
                if (inputLine.equals("Bye.")) {
                    inbye = true;           // 设置客户端结束标志
                    
                    // 通知所有客户端有用户离开
                    String leaveMessage = "系统消息: 客户端" + username + "离开了聊天室，当前在线人数: " + (ClientManager.getClientCount() - 1);
                    System.out.println(leaveMessage);
                    ClientManager.broadcast(leaveMessage, out);
                    broadcastOnlineUsers();
                    
                    break;
                } else if (inputLine.startsWith("FILE:")) {
                    // 处理文件传输
                    handleFileTransfer(inputLine);
                } else if (inputLine.equals("GET_USERS")) {
                    // 发送在线用户列表
                    sendOnlineUsers(out);
                } else {
                    String to_username = to_who(inputLine);
                    if (to_username == null) {
                        // 广播客户端消息给所有其他客户端
                        String broadcastMessage = "客户端" + username + "说: " + inputLine;
                        System.out.println(broadcastMessage);
                        ClientManager.broadcast(broadcastMessage, out);
                    } else {
                        String privateMessage = "客户端" + username + "对你说: " + inputLine.replace("$" + to_username, "");
                        System.out.println("私聊: " + username + " -> " + to_username + ": " + inputLine);
                        ClientManager.sicast(privateMessage, to_username);
                        
                        // 同时给发送者返回确认消息
                        out.println("你私聊" + to_username + ": " + inputLine.replace("$" + to_username, ""));
                        out.flush();
                    }
                }
            } while (!inbye);

        } catch (IOException e) {
            System.err.println("与客户端" + username + "通信时发生错误:");
            e.printStackTrace();
        } finally {
            // 从管理器移除客户端
            if (out != null) {
                ClientManager.removeClient(out);
            }
            
            // 广播更新后的在线用户列表
            broadcastOnlineUsers();
            
            // 关闭所有流和套接字
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("关闭客户端" + username + "连接时发生错误:");
                e.printStackTrace();
            }
            System.out.println("客户端" + clientNumber + "连接已关闭");
        }
    }
    
    /**
     * 处理文件传输
     * @param fileCommand 文件传输命令
     */
    private void handleFileTransfer(String fileCommand) {
        try {
            // 格式: FILE:target_user:filename:filedata
            String[] parts = fileCommand.split(":", 4);
            if (parts.length == 4) {
                String targetUser = parts[1];
                String filename = parts[2];
                String fileData = parts[3];
                
                System.out.println("文件传输: " + username + " -> " + targetUser + " 文件: " + filename);
                ClientManager.sendFile(filename, fileData, targetUser, username);
                
                // 给发送者确认
                PrintWriter sender = ClientManager.getClientByUsername(username);
                if (sender != null) {
                    sender.println("文件已发送给 " + targetUser + ": " + filename);
                    sender.flush();
                }
            }
        } catch (Exception e) {
            System.err.println("处理文件传输时发生错误:");
            e.printStackTrace();
        }
    }
    
    /**
     * 发送在线用户列表给指定客户端
     * @param out 客户端输出流
     */
    private void sendOnlineUsers(PrintWriter out) {
        List<String> users = ClientManager.getOnlineUsers();
        String usersMessage = "ONLINE_USERS:" + String.join(",", users);
        out.println(usersMessage);
        out.flush();
    }
    
    /**
     * 广播在线用户列表给所有客户端
     */
    private void broadcastOnlineUsers() {
        List<String> users = ClientManager.getOnlineUsers();
        String usersMessage = "ONLINE_USERS:" + String.join(",", users);
        ClientManager.broadcast(usersMessage, null);
    }
}