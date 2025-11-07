<h1 align="center">Java 程序设计实验报告</h1>

<p align="center">计算机大类 2405班 姓名 张可凡 学号 2024317220511 </p>

**实验环境** ： 普通PC机，Windows 11 系统

## MultiTalkServer.java
```java
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
```

## SingleTalkClient.java

```java 
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Base64;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class SingleTalkClient extends JFrame {
    private Socket client = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private boolean connected = false;
    private String username = "";
    
    private JTextArea messageArea; 
    private JTextField inputField; 
    private JButton sendButton;   
    private JButton fileButton;   
    private JButton usersButton;  
    private JTextField serverField; 
    private JTextField portField;   
    private JButton connectButton;  
    private JList<String> usersList;
    private DefaultListModel<String> usersListModel;

    public SingleTalkClient() {
        super("聊天客户端");
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initComponents() {
        JPanel connectPanel = new JPanel(new FlowLayout());
        connectPanel.add(new JLabel("服务器地址:"));
        serverField = new JTextField("127.0.0.1", 15);
        connectPanel.add(serverField);
        connectPanel.add(new JLabel("端口:"));
        portField = new JTextField("8898", 5);
        connectPanel.add(portField);
        connectButton = new JButton("连接");
        connectPanel.add(connectButton);
        
        // 用户列表面板
        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.setPreferredSize(new Dimension(150, 0));
        usersPanel.setBorder(BorderFactory.createTitledBorder("在线用户"));
        
        usersListModel = new DefaultListModel<>();
        usersList = new JList<>(usersListModel);
        usersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane usersScrollPane = new JScrollPane(usersList);
        usersPanel.add(usersScrollPane, BorderLayout.CENTER);
        
        JPanel usersButtonPanel = new JPanel(new FlowLayout());
        usersButton = new JButton("刷新用户");
        usersButton.setEnabled(false);
        usersButtonPanel.add(usersButton);
        usersPanel.add(usersButtonPanel, BorderLayout.SOUTH);
        
        messageArea = new JTextArea();
        messageArea.setEditable(false); 
        JScrollPane scrollPane = new JScrollPane(messageArea);
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JPanel buttonPanel = new JPanel(new FlowLayout());
        sendButton = new JButton("发送");
        sendButton.setEnabled(false); 
        fileButton = new JButton("发送文件");
        fileButton.setEnabled(false);
        buttonPanel.add(sendButton);
        buttonPanel.add(fileButton);
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(connectPanel, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(usersPanel, BorderLayout.EAST);
        contentPane.add(centerPanel, BorderLayout.CENTER);
        contentPane.add(inputPanel, BorderLayout.SOUTH);
        
        addEventListeners();
    }

    private void addEventListeners() {
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!connected) {
                    connectToServer();
                } else {
                    disconnectFromServer();
                }
            }
        });
        
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        fileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFile();
            }
        });
        
        usersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                requestOnlineUsers();
            }
        });
        
        usersList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selectedUser = usersList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(username)) {
                        inputField.setText("$" + selectedUser + " ");
                        inputField.requestFocus();
                    }
                }
            }
        });
        
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
    }

    private void connectToServer() {
        try {
            String server = serverField.getText();
            int port = Integer.parseInt(portField.getText());
            
            client = new Socket(server, port);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            connected = true;
            
            appendMessage("已连接到服务器 " + server + ":" + port);
            connectButton.setText("断开");
            sendButton.setEnabled(true);
            fileButton.setEnabled(true);
            usersButton.setEnabled(true);
            serverField.setEnabled(false);
            portField.setEnabled(false);
            inputField.requestFocus();
            
            startReceiveThread();
            
        } catch (UnknownHostException ex) {
            appendMessage("错误: 未知主机: " + serverField.getText());
        } catch (IOException ex) {
            appendMessage("错误: 无法连接到服务器");
        } catch (NumberFormatException ex) {
            appendMessage("错误: 端口号格式不正确");
        }
    }

    private void disconnectFromServer() {
        try {
            if (out != null) {
                out.println("Bye.");
                out.flush();
            }
            
            if (in != null) in.close();
            if (out != null) out.close();
            if (client != null) client.close();
            
            connected = false;
            connectButton.setText("连接");
            sendButton.setEnabled(false);
            fileButton.setEnabled(false);
            usersButton.setEnabled(false);
            serverField.setEnabled(true);
            portField.setEnabled(true);
            usersListModel.clear();
            username = "";
            appendMessage("已断开与服务器的连接");
            
        } catch (IOException ex) {
            appendMessage("错误: 断开连接时发生异常");
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            out.flush();
            
            // 不显示私聊命令，只显示实际消息内容
            String displayMessage = message;
            if (message.contains("$")) {
                // 提取实际消息内容（去掉$username部分）
                int dollarIndex = message.indexOf('$');
                int spaceIndex = message.indexOf(' ', dollarIndex);
                if (spaceIndex != -1) {
                    displayMessage = message.substring(spaceIndex + 1);
                }
                appendMessage("我(私聊): " + displayMessage);
            } else {
                appendMessage("我: " + displayMessage);
            }
            
            inputField.setText("");
            
            // 如果发送了结束消息，自动断开连接
            if (message.equals("Bye.")) {
                try {
                    Thread.sleep(100); // 等待服务器响应
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                disconnectFromServer();
            }
        }
    }
    
    private void sendFile() {
        if (!connected || out == null) {
            appendMessage("错误: 未连接到服务器");
            return;
        }
        
        String targetUser = usersList.getSelectedValue();
        if (targetUser == null || targetUser.equals(username)) {
            appendMessage("请先选择一个目标用户");
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的文件");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.length() > 1024 * 1024) { // 限制文件大小为1MB
                appendMessage("错误: 文件大小不能超过1MB");
                return;
            }
            
            try {
                // 读取文件并编码为Base64
                FileInputStream fis = new FileInputStream(file);
                byte[] fileData = new byte[(int) file.length()];
                fis.read(fileData);
                fis.close();
                
                String encodedFile = Base64.getEncoder().encodeToString(fileData);
                
                // 发送文件命令: FILE:target_user:filename:filedata
                String fileCommand = "FILE:" + targetUser + ":" + file.getName() + ":" + encodedFile;
                out.println(fileCommand);
                out.flush();
                
                appendMessage("正在发送文件给 " + targetUser + ": " + file.getName());
                
            } catch (IOException ex) {
                appendMessage("错误: 读取文件失败");
            }
        }
    }
    
    private void requestOnlineUsers() {
        if (out != null) {
            out.println("GET_USERS");
            out.flush();
        }
    }

    private void startReceiveThread() {
        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String fromServer;
                    while ((fromServer = in.readLine()) != null && connected) {
                        final String message = fromServer;
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                handleServerMessage(message);
                            }
                        });
                    }
                } catch (IOException ex) {
                    if (connected) { 
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                appendMessage("错误: 与服务器的连接已断开");
                                disconnectFromServer();
                            }
                        });
                    }
                }
            }
        });
        receiveThread.setDaemon(true); 
        receiveThread.start();
    }
    
    private void handleServerMessage(String message) {
        if (message.startsWith("WELCOME:")) {
            // 获取用户名
            username = message.substring(8);
            appendMessage("欢迎 " + username + "! 使用 $用户名 进行私聊，双击用户列表快速选择私聊对象");
        } else if (message.startsWith("ONLINE_USERS:")) {
            // 更新在线用户列表
            String usersStr = message.substring(13);
            String[] users = usersStr.split(",");
            usersListModel.clear();
            for (String user : users) {
                if (!user.isEmpty()) {
                    usersListModel.addElement(user);
                }
            }
        } else if (message.startsWith("FILE:")) {
            // 处理接收到的文件
            handleReceivedFile(message);
        } else if (message.equals("Bye.")) {
            appendMessage("服务器: " + message);
            disconnectFromServer();
        } else {
            appendMessage("服务器: " + message);
        }
    }
    
    private void handleReceivedFile(String fileMessage) {
        try {
            // 格式: FILE:from_user:filename:filedata
            String[] parts = fileMessage.split(":", 4);
            if (parts.length == 4) {
                String fromUser = parts[1];
                String filename = parts[2];
                String fileData = parts[3];
                
                // 询问用户是否保存文件
                int result = JOptionPane.showConfirmDialog(
                    this, 
                    "用户 " + fromUser + " 发送了文件: " + filename + "\n是否保存？", 
                    "接收文件", 
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new File(filename));
                    fileChooser.setDialogTitle("保存文件");
                    
                    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        File file = fileChooser.getSelectedFile();
                        
                        // 解码Base64数据并保存文件
                        byte[] decodedData = Base64.getDecoder().decode(fileData);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(decodedData);
                        fos.close();
                        
                        appendMessage("文件已保存: " + file.getAbsolutePath());
                    }
                }
            }
        } catch (Exception ex) {
            appendMessage("错误: 接收文件失败");
        }
    }

    private void appendMessage(final String message) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                messageArea.append(message + "\n");
                messageArea.setCaretPosition(messageArea.getDocument().getLength());
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SingleTalkClient();
            }
        });
    }
}
```

## SingleTalkServer.java

```java
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

```