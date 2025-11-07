import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

/**
 * QQ主界面 - 包含好友列表和聊天窗口
 */
public class QQMainFrame extends JFrame {
    private String username;
    private DefaultListModel<String> friendListModel;
    private JList<String> friendList;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private Map<String, JTextArea> chatWindows;
    
    public QQMainFrame(String username) {
        this.username = username;
        this.chatWindows = new HashMap<>();
        initComponents();
        setupLayout();
        setupListeners();
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
        statusPanel.add(new JLabel("在线状态: ● 在线"));
        
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
                logout();
            }
        });
    }
    
    private void loadFriends() {
        // 模拟加载好友列表
        friendListModel.addElement("user1");
        friendListModel.addElement("user2");
        friendListModel.addElement("user3");
        friendListModel.addElement("群聊");
        
        // 为每个好友创建聊天窗口
        for (int i = 0; i < friendListModel.size(); i++) {
            String friend = friendListModel.getElementAt(i);
            chatWindows.put(friend, new JTextArea());
        }
    }
    
    private void showChatWithFriend(String friend) {
        setTitle("Java QQ - " + username + " (正在与 " + friend + " 聊天)");
        
        // 清空当前聊天区域
        chatArea.setText("");
        
        // 显示与选中好友的聊天记录
        JTextArea friendChatArea = chatWindows.get(friend);
        if (friendChatArea != null) {
            chatArea.setText(friendChatArea.getText());
        }
        
        // 添加一些模拟聊天记录
        if (friendChatArea.getText().isEmpty()) {
            String welcomeMessage = "=== 开始与 " + friend + " 聊天 ===\n";
            if (friend.equals("群聊")) {
                welcomeMessage += "这是群聊窗口，所有消息将发送给所有在线用户\n";
            } else {
                welcomeMessage += "这是私聊窗口，只有你们两人能看到消息\n";
            }
            welcomeMessage += "========================\n\n";
            friendChatArea.setText(welcomeMessage);
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
        
        // 添加到聊天记录
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        String formattedMessage = username + " (" + timestamp + "): " + message + "\n";
        
        JTextArea friendChatArea = chatWindows.get(selectedFriend);
        if (friendChatArea != null) {
            friendChatArea.append(formattedMessage);
            chatArea.append(formattedMessage);
        }
        
        // 清空输入框
        messageField.setText("");
        
        // 模拟对方回复（在实际应用中应该通过服务器接收）
        if (!selectedFriend.equals("群聊")) {
            new Thread(() -> {
                try {
                    Thread.sleep(1000 + (int)(Math.random() * 2000)); // 随机延迟1-3秒
                    SwingUtilities.invokeLater(() -> {
                        String replyTimestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
                        String replyMessage = selectedFriend + " (" + replyTimestamp + "): 收到你的消息: \"" + message + "\"\n";
                        friendChatArea.append(replyMessage);
                        chatArea.append(replyMessage);
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    private void logout() {
        int result = JOptionPane.showConfirmDialog(this,
            "确定要退出Java QQ吗？",
            "退出确认",
            JOptionPane.YES_NO_OPTION);
        
        if (result == JOptionPane.YES_OPTION) {
            // 在实际应用中，这里应该发送退出消息到服务器
            System.exit(0);
        }
    }
    
    public static void main(String[] args) {
        // 测试主界面
        SwingUtilities.invokeLater(() -> {
            QQMainFrame mainFrame = new QQMainFrame("测试用户");
            mainFrame.setVisible(true);
        });
    }
}