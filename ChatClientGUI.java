import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

/**
 * 聊天客户端图形界面
 */
public class ChatClientGUI extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    
    // 界面组件
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton connectButton;
    private JTextField serverField;
    private JTextField portField;
    private JTextField usernameField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    
    private volatile boolean connected = false;
    private String username;
    
    public ChatClientGUI() {
        initializeGUI();
    }
    
    private void initializeGUI() {
        setTitle("Java QQ 客户端");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // 连接面板
        JPanel connectionPanel = createConnectionPanel();
        mainPanel.add(connectionPanel, BorderLayout.NORTH);
        
        // 聊天区域
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setBackground(new Color(240, 240, 240));
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        mainPanel.add(chatScrollPane, BorderLayout.CENTER);
        
        // 用户列表
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setPreferredSize(new Dimension(150, 0));
        JScrollPane userScrollPane = new JScrollPane(userList);
        mainPanel.add(userScrollPane, BorderLayout.EAST);
        
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
            
            // 启用输入组件
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            connectButton.setEnabled(false);
            
            // 启动消息接收线程
            new Thread(this::receiveMessages).start();
            
            // 发送用户名
            out.println(username);
            
            appendToChat("系统", "已连接到服务器 " + server + ":" + port);
            
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
        } catch (IOException e) {
            System.err.println("断开连接时出错: " + e.getMessage());
        }
        
        // 禁用输入组件
        inputField.setEnabled(false);
        sendButton.setEnabled(false);
        connectButton.setEnabled(true);
        
        appendToChat("系统", "已断开与服务器的连接");
        userListModel.clear();
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && connected) {
            out.println(message);
            appendToChat("我", message);
            inputField.setText("");
        }
    }
    
    private void receiveMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null && connected) {
                // 处理服务器消息
                if (message.startsWith("在线用户:")) {
                    updateUserList(message);
                } else if (message.contains("加入了聊天室")) {
                    appendToChat("系统", message);
                } else if (message.contains("离开了聊天室")) {
                    appendToChat("系统", message);
                } else if (message.startsWith("用户") && message.contains("对你说:")) {
                    // 私聊消息，特殊显示
                    appendToChat("私聊", message);
                } else {
                    appendToChat("聊天", message);
                }
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    appendToChat("系统", "与服务器的连接已断开");
                    disconnect();
                });
            }
        }
    }
    
    private void appendToChat(String type, String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            String formattedMessage = String.format("[%s] %s: %s\n", timestamp, type, message);
            
            // 根据消息类型设置颜色
            if (type.equals("私聊")) {
                chatArea.append(formattedMessage);
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            } else if (type.equals("系统")) {
                chatArea.append(formattedMessage);
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            } else {
                chatArea.append(formattedMessage);
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }
    
    private void updateUserList(String userListMessage) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            // 解析用户列表消息格式: "在线用户: 用户1 用户2 用户3"
            String[] users = userListMessage.substring(5).split(" ");
            for (String user : users) {
                if (!user.trim().isEmpty()) {
                    userListModel.addElement(user.trim());
                }
            }
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new ChatClientGUI().setVisible(true);
        });
    }
}