import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

/**
 * 集成图形界面和网络通信的QQ客户端
 */
public class QQIntegratedClient extends JFrame {
    private String username;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    
    // GUI组件
    private DefaultListModel<String> friendListModel;
    private JList<String> friendList;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private Map<String, StringBuilder> chatHistories;
    
    public QQIntegratedClient(String username) {
        this.username = username;
        this.chatHistories = new HashMap<>();
        initComponents();
        setupLayout();
        setupListeners();
        connectToServer();
        loadFriends();
    }
    
    private void initComponents() {
        setTitle("Java QQ - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // 创建好友列表模型
        friendListModel = new DefaultListModel<>();
        friendList = new JList<>(friendListModel);
        friendList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendList.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        // 创建聊天区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        // 创建消息输入区域
        messageField = new JTextField();
        sendButton = new JButton("发送");
        sendButton.setBackground(new Color(0, 120, 215));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 左侧好友列表
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder("好友列表"));
        
        JScrollPane friendScrollPane = new JScrollPane(friendList);
        leftPanel.add(friendScrollPane, BorderLayout.CENTER);
        
        // 右侧聊天区域
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // 聊天显示区域
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createTitledBorder("聊天窗口"));
        rightPanel.add(chatScrollPane, BorderLayout.CENTER);
        
        // 消息输入面板
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        rightPanel.add(inputPanel, BorderLayout.SOUTH);
        
        // 添加分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(200);
        splitPane.setResizeWeight(0.0);
        
        add(splitPane, BorderLayout.CENTER);
        
        // 状态栏
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("当前用户: " + username));
        statusPanel.add(Box.createHorizontalStrut(20));
        statusPanel.add(new JLabel("服务器状态: " + (connected ? "● 已连接" : "● 未连接")));
        
        add(statusPanel, BorderLayout.SOUTH);
    }
    
    private void setupListeners() {
        // 发送按钮事件
        sendButton.addActionListener(e -> sendMessage());
        
        // 回车键发送消息
        messageField.addActionListener(e -> sendMessage());
        
        // 好友列表选择事件
        friendList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedFriend = friendList.getSelectedValue();
                if (selectedFriend != null) {
                    showChatWithFriend(selectedFriend);
                }
            }
        });
        
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }
    
    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket("127.0.0.1", 8898);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connected = true;
                
                // 启动消息接收线程
                Thread receiverThread = new Thread(this::receiveMessages);
                receiverThread.setDaemon(true);
                receiverThread.start();
                
                // 更新状态栏
                SwingUtilities.invokeLater(() -> {
                    updateStatus("服务器状态: ● 已连接");
                });
                
            } catch (IOException e) {
                connected = false;
                SwingUtilities.invokeLater(() -> {
                    updateStatus("服务器状态: ● 连接失败");
                    JOptionPane.showMessageDialog(this,
                        "无法连接到服务器: " + e.getMessage() + "\n请确保服务器正在运行。",
                        "连接错误",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                final String finalMessage = message;
                SwingUtilities.invokeLater(() -> {
                    processServerMessage(finalMessage);
                });
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    updateStatus("服务器状态: ● 连接断开");
                    JOptionPane.showMessageDialog(this,
                        "与服务器的连接已断开",
                        "连接错误",
                        JOptionPane.WARNING_MESSAGE);
                });
            }
        }
    }
    
    private void processServerMessage(String message) {
        // 处理不同类型的服务器消息
        if (message.startsWith("[私聊]")) {
            // 私聊消息格式: [私聊] 发送者: 消息内容
            chatArea.append(message + "\n");
        } else if (message.startsWith("[群聊]")) {
            // 群聊消息格式: [群聊] 发送者: 消息内容
            chatArea.append(message + "\n");
        } else if (message.startsWith("[系统]")) {
            // 系统消息
            chatArea.append(message + "\n");
        } else if (message.contains("在线用户列表")) {
            // 在线用户列表
            // 在实际应用中，这里应该解析用户列表并更新好友列表
        } else {
            // 其他消息
            chatArea.append(message + "\n");
        }
        
        // 自动滚动到底部
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    private void loadFriends() {
        // 模拟加载好友列表
        friendListModel.addElement("user1");
        friendListModel.addElement("user2");
        friendListModel.addElement("user3");
        friendListModel.addElement("群聊");
        
        // 为每个好友创建聊天历史
        for (int i = 0; i < friendListModel.size(); i++) {
            String friend = friendListModel.getElementAt(i);
            chatHistories.put(friend, new StringBuilder());
        }
    }
    
    private void showChatWithFriend(String friend) {
        setTitle("Java QQ - " + username + " (正在与 " + friend + " 聊天)");
        
        // 清空当前聊天区域
        chatArea.setText("");
        
        // 显示与选中好友的聊天记录
        StringBuilder history = chatHistories.get(friend);
        if (history != null) {
            chatArea.setText(history.toString());
        }
        
        // 添加欢迎消息
        if (history.length() == 0) {
            String welcomeMessage = "=== 开始与 " + friend + " 聊天 ===\n";
            if (friend.equals("群聊")) {
                welcomeMessage += "这是群聊窗口，所有消息将发送给所有在线用户\n";
            } else {
                welcomeMessage += "这是私聊窗口，只有你们两人能看到消息\n";
            }
            welcomeMessage += "========================\n\n";
            history.append(welcomeMessage);
            chatArea.setText(welcomeMessage);
        }
    }
    
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) {
            return;
        }
        
        String selectedFriend = friendList.getSelectedValue();
        if (selectedFriend == null) {
            JOptionPane.showMessageDialog(this, "请先选择一个好友或群聊！");
            return;
        }
        
        if (!connected) {
            JOptionPane.showMessageDialog(this, "未连接到服务器，无法发送消息！");
            return;
        }
        
        // 构建发送格式
        String formattedMessage;
        if (selectedFriend.equals("群聊")) {
            formattedMessage = "all " + message;
        } else {
            formattedMessage = "@" + selectedFriend + " " + message;
        }
        
        // 发送到服务器
        out.println(formattedMessage);
        
        // 添加到本地聊天记录
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        String localMessage = username + " (" + timestamp + "): " + message + "\n";
        
        StringBuilder history = chatHistories.get(selectedFriend);
        if (history != null) {
            history.append(localMessage);
            chatArea.append(localMessage);
        }
        
        // 清空输入框
        messageField.setText("");
        
        // 自动滚动到底部
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    private void updateStatus(String status) {
        // 更新状态栏
        Component[] components = ((JPanel)getContentPane().getComponent(1)).getComponents();
        for (Component comp : components) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label.getText().startsWith("服务器状态:")) {
                    label.setText(status);
                    break;
                }
            }
        }
    }
    
    private void disconnect() {
        connected = false;
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
    
    public static void main(String[] args) {
        // 测试集成客户端
        SwingUtilities.invokeLater(() -> {
            QQIntegratedClient client = new QQIntegratedClient("测试用户");
            client.setVisible(true);
        });
    }
}