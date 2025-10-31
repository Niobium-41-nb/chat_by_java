import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * 增强版聊天客户端 - 带图形用户界面
 * 支持：登录界面、好友管理、聊天界面、并行通信
 */
public class EnhancedChatClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    
    // GUI 组件
    private JFrame loginFrame;
    private JFrame mainFrame;
    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> friendList;
    private DefaultListModel<String> friendListModel;
    private JList<String> onlineList;
    private DefaultListModel<String> onlineListModel;
    
    // 线程池用于处理消息接收
    private ExecutorService messageExecutor;
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EnhancedChatClient().showLogin();
        });
    }
    
    public EnhancedChatClient() {
        messageExecutor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * 显示登录界面
     */
    private void showLogin() {
        loginFrame = new JFrame("聊天系统 - 登录");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(350, 250);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 标题
        JLabel titleLabel = new JLabel("聊天系统登录", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 输入面板
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        
        JLabel userLabel = new JLabel("用户名:");
        userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JTextField userField = new JTextField();
        userField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        JLabel serverLabel = new JLabel("服务器:");
        serverLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JTextField serverField = new JTextField("127.0.0.1");
        serverField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        JLabel portLabel = new JLabel("端口:");
        portLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        JTextField portField = new JTextField("8898");
        portField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        inputPanel.add(userLabel);
        inputPanel.add(userField);
        inputPanel.add(serverLabel);
        inputPanel.add(serverField);
        inputPanel.add(portLabel);
        inputPanel.add(portField);
        
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton loginButton = new JButton("登录");
        JButton cancelButton = new JButton("取消");
        
        loginButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        cancelButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 登录按钮事件
        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String server = serverField.getText().trim();
            String portStr = portField.getText().trim();
            
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "请输入用户名", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                int port = Integer.parseInt(portStr);
                if (connectToServer(server, port, username)) {
                    loginFrame.dispose();
                    showMainInterface();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(loginFrame, "端口号格式错误", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // 取消按钮事件
        cancelButton.addActionListener(e -> System.exit(0));
        
        // 回车键登录
        userField.addActionListener(e -> loginButton.doClick());
        serverField.addActionListener(e -> loginButton.doClick());
        portField.addActionListener(e -> loginButton.doClick());
        
        loginFrame.add(mainPanel);
        loginFrame.setVisible(true);
        
        // 设置焦点
        userField.requestFocus();
    }
    
    /**
     * 连接到服务器
     */
    private boolean connectToServer(String server, int port, String username) {
        try {
            socket = new Socket(server, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // 读取服务器欢迎消息
            String response = in.readLine();
            if (response.startsWith("请输入用户名:")) {
                out.println(username);
                response = in.readLine();
                
                if (response.startsWith("SUCCESS:")) {
                    this.username = username;
                    startMessageReceiver();
                    return true;
                } else {
                    JOptionPane.showMessageDialog(loginFrame, response, "登录失败", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(loginFrame, "连接服务器失败: " + e.getMessage(), 
                                         "连接错误", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
    
    /**
     * 启动消息接收线程
     */
    private void startMessageReceiver() {
        messageExecutor.execute(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> handleServerMessage(finalMessage));
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    if (mainFrame != null && mainFrame.isVisible()) {
                        JOptionPane.showMessageDialog(mainFrame, "与服务器连接断开", 
                                                     "连接错误", JOptionPane.ERROR_MESSAGE);
                        System.exit(0);
                    }
                });
            }
        });
    }
    
    /**
     * 处理服务器消息
     */
    private void handleServerMessage(String message) {
        if (message.startsWith("FRIENDS:")) {
            // 好友列表更新
            updateFriendList(message.substring(8).trim());
        } else if (message.startsWith("ONLINE:")) {
            // 在线用户列表更新
            updateOnlineList(message.substring(7).trim());
        } else if (message.startsWith("SYSTEM:")) {
            // 系统消息
            chatArea.append("【系统】" + message.substring(7) + "\n");
        } else if (message.startsWith("ERROR:")) {
            // 错误消息
            JOptionPane.showMessageDialog(mainFrame, message.substring(6), 
                                         "错误", JOptionPane.ERROR_MESSAGE);
        } else {
            // 普通聊天消息
            chatArea.append(message + "\n");
        }
        
        // 自动滚动到底部
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    /**
     * 更新好友列表
     */
    private void updateFriendList(String friendInfo) {
        if (friendInfo.startsWith("你的好友列表:")) {
            friendListModel.clear();
        } else if (!friendInfo.isEmpty() && !friendInfo.equals("你还没有好友")) {
            friendListModel.addElement(friendInfo.trim());
        }
    }
    
    /**
     * 更新在线用户列表
     */
    private void updateOnlineList(String onlineInfo) {
        if (onlineInfo.startsWith("当前在线用户")) {
            onlineListModel.clear();
        } else if (!onlineInfo.isEmpty()) {
            onlineListModel.addElement(onlineInfo.trim());
        }
    }
    
    /**
     * 显示主界面
     */
    private void showMainInterface() {
        mainFrame = new JFrame("聊天系统 - " + username);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(800, 600);
        mainFrame.setLocationRelativeTo(null);
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 左侧面板 - 好友列表和在线用户
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder("联系人"));
        
        // 好友列表
        friendListModel = new DefaultListModel<>();
        friendList = new JList<>(friendListModel);
        friendList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        JScrollPane friendScroll = new JScrollPane(friendList);
        friendScroll.setBorder(BorderFactory.createTitledBorder("好友列表"));
        
        // 在线用户列表
        onlineListModel = new DefaultListModel<>();
        onlineList = new JList<>(onlineListModel);
        onlineList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        JScrollPane onlineScroll = new JScrollPane(onlineList);
        onlineScroll.setBorder(BorderFactory.createTitledBorder("在线用户"));
        
        leftPanel.add(friendScroll, BorderLayout.CENTER);
        leftPanel.add(onlineScroll, BorderLayout.SOUTH);
        
        // 中间面板 - 聊天区域
        JPanel centerPanel = new JPanel(new BorderLayout());
        
        // 聊天显示区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createTitledBorder("聊天内容"));
        
        // 消息输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        JButton sendButton = new JButton("发送");
        sendButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        centerPanel.add(chatScroll, BorderLayout.CENTER);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // 右侧面板 - 功能按钮
        JPanel rightPanel = new JPanel(new GridLayout(6, 1, 5, 5));
        rightPanel.setPreferredSize(new Dimension(120, 0));
        rightPanel.setBorder(BorderFactory.createTitledBorder("功能"));
        
        JButton refreshFriendsBtn = new JButton("刷新好友");
        JButton refreshOnlineBtn = new JButton("刷新在线");
        JButton groupChatBtn = new JButton("群发消息");
        JButton fileTransferBtn = new JButton("文件传输");
        JButton clearChatBtn = new JButton("清空聊天");
        JButton quitBtn = new JButton("退出");
        
        // 设置按钮字体
        Font buttonFont = new Font("微软雅黑", Font.PLAIN, 12);
        refreshFriendsBtn.setFont(buttonFont);
        refreshOnlineBtn.setFont(buttonFont);
        groupChatBtn.setFont(buttonFont);
        fileTransferBtn.setFont(buttonFont);
        clearChatBtn.setFont(buttonFont);
        quitBtn.setFont(buttonFont);
        
        rightPanel.add(refreshFriendsBtn);
        rightPanel.add(refreshOnlineBtn);
        rightPanel.add(groupChatBtn);
        rightPanel.add(fileTransferBtn);
        rightPanel.add(clearChatBtn);
        rightPanel.add(quitBtn);
        
        // 添加组件到主面板
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(rightPanel, BorderLayout.EAST);
        
        // 设置事件监听器
        setupEventListeners(sendButton, refreshFriendsBtn, refreshOnlineBtn, 
                           groupChatBtn, fileTransferBtn, clearChatBtn, quitBtn);
        
        mainFrame.add(mainPanel);
        mainFrame.setVisible(true);
        
        // 设置输入框焦点
        messageField.requestFocus();
    }
    
    /**
     * 设置事件监听器
     */
    private void setupEventListeners(JButton sendButton, JButton refreshFriendsBtn,
                                   JButton refreshOnlineBtn, JButton groupChatBtn,
                                   JButton fileTransferBtn, JButton clearChatBtn,
                                   JButton quitBtn) {
        // 发送消息
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
        
        // 刷新好友列表
        refreshFriendsBtn.addActionListener(e -> out.println("/friends"));
        
        // 刷新在线用户
        refreshOnlineBtn.addActionListener(e -> out.println("/online"));
        
        // 群发消息
        groupChatBtn.addActionListener(e -> {
            String message = JOptionPane.showInputDialog(mainFrame, "请输入群发消息:");
            if (message != null && !message.trim().isEmpty()) {
                out.println("/group " + message);
            }
        });
        
        // 文件传输
        fileTransferBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(mainFrame, "文件传输功能正在开发中...", 
                                         "提示", JOptionPane.INFORMATION_MESSAGE);
        });
        
        // 清空聊天
        clearChatBtn.addActionListener(e -> chatArea.setText(""));
        
        // 退出
        quitBtn.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(mainFrame, "确定要退出聊天吗?", 
                                                      "确认退出", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                out.println("/quit");
                System.exit(0);
            }
        });
        
        // 双击好友列表发送私聊
        friendList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedFriend = friendList.getSelectedValue();
                    if (selectedFriend != null) {
                        // 提取用户名（去掉状态信息）
                        String friendName = selectedFriend.split(" ")[0].trim();
                        messageField.setText("@" + friendName + " ");
                        messageField.requestFocus();
                    }
                }
            }
        });
        
        // 双击在线用户列表发送私聊
        onlineList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    String selectedUser = onlineList.getSelectedValue();
                    if (selectedUser != null) {
                        messageField.setText("@" + selectedUser + " ");
                        messageField.requestFocus();
                    }
                }
            }
        });
    }
    
    /**
     * 发送消息
     */
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            messageField.setText("");
        }
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) socket.close();
            if (messageExecutor != null) messageExecutor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}