import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

/**
 * 增强版聊天客户端
 * 支持文件传输和群聊功能
 */
public class EnhancedChatClient extends JFrame {
    private Socket socket;
    private Socket fileSocket;
    private PrintWriter out;
    private BufferedReader in;
    
    // 界面组件
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton connectButton;
    private JButton fileButton;
    private JTextField serverField;
    private JTextField portField;
    private JTextField usernameField;
    private JList<String> userList;
    private JList<String> groupList;
    private DefaultListModel<String> userListModel;
    private DefaultListModel<String> groupListModel;
    private JTabbedPane tabbedPane;
    
    private volatile boolean connected = false;
    private String username;
    private int clientId;
    private Map<String, Set<String>> groupMembers;
    
    // 文件接收相关
    private String pendingFileName;
    private long pendingFileSize;
    private String pendingFileSender;
    private String pendingFileAddress;
    
    public EnhancedChatClient() {
        initializeGUI();
        groupMembers = new HashMap<>();
    }
    
    private void initializeGUI() {
        setTitle("Java QQ 增强版客户端");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 连接面板
        JPanel connectionPanel = createConnectionPanel();
        mainPanel.add(connectionPanel, BorderLayout.NORTH);
        
        // 中央区域 - 使用选项卡
        tabbedPane = new JTabbedPane();
        
        // 聊天区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(240, 240, 240));
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        tabbedPane.addTab("聊天", chatScrollPane);
        
        // 用户列表
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addMouseListener(new UserListMouseListener());
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(200, 0));
        
        // 群组列表
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.addMouseListener(new GroupListMouseListener());
        JScrollPane groupScrollPane = new JScrollPane(groupList);
        groupScrollPane.setPreferredSize(new Dimension(200, 0));
        
        // 右侧面板 - 用户和群组列表
        JPanel rightPanel = new JPanel(new GridLayout(2, 1));
        rightPanel.add(userScrollPane);
        rightPanel.add(groupScrollPane);
        
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        // 输入面板
        JPanel inputPanel = createInputPanel();
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // 添加事件监听器
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }
    
    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout());
        panel.setBorder(BorderFactory.createTitledBorder("连接设置"));
        
        panel.add(new JLabel("服务器:"));
        serverField = new JTextField("127.0.0.1", 10);
        panel.add(serverField);
        
        panel.add(new JLabel("端口:"));
        portField = new JTextField("8898", 5);
        panel.add(portField);
        
        panel.add(new JLabel("用户名:"));
        usernameField = new JTextField("用户", 8);
        panel.add(usernameField);
        
        connectButton = new JButton("连接");
        connectButton.addActionListener(e -> connectToServer());
        panel.add(connectButton);
        
        JButton disconnectButton = new JButton("断开");
        disconnectButton.addActionListener(e -> disconnect());
        panel.add(disconnectButton);
        
        fileButton = new JButton("发送文件");
        fileButton.setEnabled(false);
        fileButton.addActionListener(e -> sendFile());
        panel.add(fileButton);
        
        return panel;
    }
    
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        inputField = new JTextField();
        inputField.setEnabled(false);
        inputField.addActionListener(e -> sendMessage());
        
        sendButton = new JButton("发送");
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());
        
        panel.add(inputField, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        
        return panel;
    }
    
    private void connectToServer() {
        try {
            String server = serverField.getText();
            int port = Integer.parseInt(portField.getText());
            username = usernameField.getText().trim();
            
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "请输入用户名", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            socket = new Socket(server, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            connected = true;
            
            // 发送用户名
            out.println(username);
            
            // 启用输入组件
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            fileButton.setEnabled(true);
            connectButton.setEnabled(false);
            
            // 启动消息接收线程
            new Thread(this::receiveMessages).start();
            
            appendToChat("系统", "已连接到服务器 " + server + ":" + port, Color.BLUE);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "连接失败: " + e.getMessage(), 
                "连接错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void disconnect() {
        connected = false;
        
        if (out != null) {
            out.println("Bye.");
            out.close();
        }
        
        try {
            if (in != null) in.close();
            if (socket != null) socket.close();
            if (fileSocket != null) fileSocket.close();
        } catch (IOException e) {
            System.err.println("断开连接时出错: " + e.getMessage());
        }
        
        // 禁用输入组件
        inputField.setEnabled(false);
        sendButton.setEnabled(false);
        fileButton.setEnabled(false);
        connectButton.setEnabled(true);
        
        appendToChat("系统", "已断开与服务器的连接", Color.BLUE);
        userListModel.clear();
        groupListModel.clear();
        groupMembers.clear();
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && connected) {
            out.println(message);
            
            // 在聊天区域显示自己发送的消息
            if (message.startsWith("@")) {
                // 私聊消息
                appendToChat("我(私聊)", message, new Color(0, 100, 0));
            } else if (message.startsWith("#")) {
                // 群聊消息
                appendToChat("我(群聊)", message, new Color(139, 0, 139));
            } else {
                // 公共消息
                appendToChat("我", message, Color.DARK_GRAY);
            }
            
            inputField.setText("");
        }
    }
    
    private void sendFile() {
        String selectedUser = userList.getSelectedValue();
        if (selectedUser == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个用户", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 提取用户名（去除ID部分）
        String targetUsername = selectedUser.split("\\(")[0].trim();
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择要发送的文件");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();
            
            // 使用文件命令发送文件
            out.println("/file send " + targetUsername + " " + filePath);
        }
    }
    
    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null && connected) {
                handleServerMessage(message);
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    appendToChat("系统", "与服务器的连接已断开", Color.RED);
                    disconnect();
                });
            }
        }
    }
    
    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.startsWith("WELCOME:")) {
                String welcomeMsg = message.substring(8);
                appendToChat("系统", welcomeMsg, Color.BLUE);
                
            } else if (message.startsWith("ONLINE_USERS:")) {
                updateUserList(message.substring(13));
                
            } else if (message.startsWith("GROUP_LIST:")) {
                updateGroupList(message.substring(11));
                
            } else if (message.startsWith("PUBLIC:")) {
                String[] parts = message.split(":", 3);
                if (parts.length >= 3) {
                    appendToChat(parts[1], parts[2], Color.BLACK);
                }
                
            } else if (message.startsWith("PRIVATE:")) {
                String[] parts = message.split(":", 3);
                if (parts.length >= 3) {
                    appendToChat(parts[1] + "(私聊)", parts[2], new Color(0, 100, 0));
                }
                
            } else if (message.startsWith("GROUP:")) {
                String[] parts = message.split(":", 3);
                if (parts.length >= 3) {
                    appendToChat(parts[1] + "群-" + parts[2].split(":")[0], 
                                parts[2].substring(parts[2].indexOf(":") + 1), 
                                new Color(139, 0, 139));
                }
                
            } else if (message.startsWith("GROUP_SYSTEM:")) {
                String[] parts = message.split(":", 3);
                if (parts.length >= 3) {
                    appendToChat(parts[1] + "群-系统", parts[2], Color.BLUE);
                }
                
            } else if (message.startsWith("GROUP_JOIN:")) {
                String[] parts = message.split(":", 3);
                if (parts.length >= 3) {
                    appendToChat("系统", "已加入群组: " + parts[2], Color.BLUE);
                }
                
            } else if (message.startsWith("FILE_REQUEST:")) {
                handleFileRequest(message.substring(13));
                
            } else {
                appendToChat("系统", message, Color.BLUE);
            }
        });
    }
    
    private void handleFileRequest(String fileInfo) {
        String[] parts = fileInfo.split(":");
        if (parts.length >= 4) {
            pendingFileSender = parts[0];
            pendingFileName = parts[1];
            pendingFileSize = Long.parseLong(parts[2]);
            pendingFileAddress = parts[3];
            
            int choice = JOptionPane.showConfirmDialog(this,
                "用户 " + pendingFileSender + " 想要发送文件:\n" +
                "文件名: " + pendingFileName + "\n" +
                "文件大小: " + formatFileSize(pendingFileSize) + "\n" +
                "是否接收？",
                "文件传输请求",
                JOptionPane.YES_NO_OPTION);
            
            if (choice == JOptionPane.YES_OPTION) {
                new Thread(this::receiveFile).start();
            }
        }
    }
    
    private void receiveFile() {
        try {
            fileSocket = new Socket(pendingFileAddress, 8899);
            DataOutputStream dos = new DataOutputStream(fileSocket.getOutputStream());
            DataInputStream dis = new DataInputStream(fileSocket.getInputStream());
            
            // 发送文件请求
            dos.writeUTF("REQUEST_FILE");
            dos.writeUTF(pendingFileName);
            dos.flush();
            
            // 读取文件大小和准备状态
            long fileSize = dis.readLong();
            String status = dis.readUTF();
            
            if ("READY".equals(status) && fileSize > 0) {
                // 选择保存位置
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setSelectedFile(new File(pendingFileName));
                fileChooser.setDialogTitle("保存文件");
                
                if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                    File saveFile = fileChooser.getSelectedFile();
                    
                    FileOutputStream fos = new FileOutputStream(saveFile);
                    byte[] buffer = new byte[4096];
                    long totalRead = 0;
                    int read;
                    
                    while (totalRead < fileSize && (read = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                    }
                    
                    fos.close();
                    
                    SwingUtilities.invokeLater(() -> {
                        appendToChat("系统", "文件接收完成: " + saveFile.getName(), Color.BLUE);
                    });
                }
            }
            
            fileSocket.close();
            
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                appendToChat("系统", "文件接收失败: " + e.getMessage(), Color.RED);
            });
        }
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        else if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        else return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
    
    private void updateUserList(String usersString) {
        userListModel.clear();
        String[] users = usersString.split(" ");
        for (String user : users) {
            if (!user.trim().isEmpty()) {
                userListModel.addElement(user.trim());
            }
        }
    }
    
    private void updateGroupList(String groupsString) {
        groupListModel.clear();
        String[] groups = groupsString.split(";");
        for (String group : groups) {
            if (!group.trim().isEmpty()) {
                String[] groupInfo = group.split(":");
                if (groupInfo.length >= 3) {
                    groupListModel.addElement(groupInfo[1] + " (" + groupInfo[2] + "人)");
                }
            }
        }
    }
    
    private void appendToChat(String sender, String message, Color color) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String formattedMessage = String.format("[%s] %s: %s\n", timestamp, sender, message);
        
        chatArea.append(formattedMessage);
        
        // 设置颜色（简化实现，实际可能需要使用StyledDocument）
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    // 用户列表鼠标监听器
    private class UserListMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    String username = selectedUser.split("\\(")[0].trim();
                    inputField.setText("@" + username + " ");
                    inputField.requestFocus();
                }
            }
        }
    }
    
    // 群组列表鼠标监听器
    private class GroupListMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                String selectedGroup = groupList.getSelectedValue();
                if (selectedGroup != null) {
                    String groupName = selectedGroup.split(" ")[0];
                    inputField.setText("#" + groupName + " ");
                    inputField.requestFocus();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new EnhancedChatClient().setVisible(true);
        });
    }
}