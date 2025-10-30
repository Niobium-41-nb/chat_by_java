import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * 带Swing界面的聊天客户端类
 * 连接到服务器进行聊天通信，具有图形化界面
 */
public class SingleTalkClient extends JFrame {
    // 网络相关变量
    private Socket client = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private boolean connected = false;
    private boolean sbye = false;         // 服务器结束标志
    private boolean ubye = false;         // 用户结束标志
    
    // GUI组件
    private JTextArea chatArea;           // 聊天显示区域
    private JTextField inputField;        // 输入区域
    private JButton sendButton;           // 发送按钮
    private JLabel statusLabel;           // 状态标签
    private JPanel bottomPanel;           // 底部面板
    private JScrollPane scrollPane;       // 滚动面板
    
    /**
     * 构造函数，初始化GUI界面
     */
    public SingleTalkClient() {
        super("聊天客户端");
        initComponents();
        initNetwork();
    }
    
    /**
     * 初始化GUI组件
     */
    private void initComponents() {
        // 设置窗口属性
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null); // 窗口居中
        
        // 创建聊天显示区域
        chatArea = new JTextArea();
        chatArea.setEditable(false); // 聊天区域不可编辑
        chatArea.setLineWrap(true);  // 自动换行
        chatArea.setWrapStyleWord(true); // 按单词换行
        
        // 创建滚动面板，包裹聊天区域
        scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        // 创建输入区域和发送按钮
        inputField = new JTextField();
        inputField.setEnabled(false); // 初始时禁用输入框
        sendButton = new JButton("发送");
        sendButton.setEnabled(false); // 初始时禁用发送按钮
        
        // 创建底部面板，放置输入框和发送按钮
        bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 创建状态标签
        statusLabel = new JLabel("正在连接服务器...");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // 设置布局并添加组件
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);
        
        // 添加事件监听器
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        // 添加窗口关闭监听器
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeConnection();
            }
        });
    }
    
    /**
     * 初始化网络连接
     */
    final String HOST = "127.0.0.1";
    private void initNetwork() {
        try {
            client = new Socket(HOST, 8898);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            connected = true;
            statusLabel.setText("已连接到服务器 127.0.0.1:8898");
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            inputField.requestFocus();
            
            // 创建线程接收服务器消息
            new Thread(new MessageReceiver()).start();
        } catch (UnknownHostException ex) {
            statusLabel.setText("错误: 未知主机 127.0.0.1");
            appendToChat("错误: 未知主机 127.0.0.1\n");
        } catch (IOException ex) {
            statusLabel.setText("错误: 无法连接到服务器");
            appendToChat("错误: 无法连接到服务器 127.0.0.1:8898\n");
        }
    }
    
    /**
     * 发送消息到服务器
     */
    private void sendMessage() {
        if (!connected || ubye) return;
        
        String message = inputField.getText().trim();
        if (message.isEmpty()) return;
        
        out.println(message);
        // appendToChat("我: " + message + "\n");
        
        if (message.equals("Bye.")) {
            ubye = true;
            statusLabel.setText("等待服务器确认断开连接...");
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
        }
        
        inputField.setText(""); // 清空输入框
    }
    
    /**
     * 将文本追加到聊天区域
     */
    private void appendToChat(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                chatArea.append(text);
                // 自动滚动到底部
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
            }
        });
    }
    
    /**
     * 关闭网络连接
     */
    private void closeConnection() {
        try {
            if (out != null) {
                if (!ubye) {
                    out.println("Bye.");
                }
                out.close();
            }
            if (in != null) in.close();
            if (client != null && !client.isClosed()) client.close();
            connected = false;
            appendToChat("客户端已断开连接\n");
        } catch (IOException ex) {
            appendToChat("关闭连接时发生错误\n");
        }
    }
    
    /**
     * 消息接收线程类，用于接收服务器消息
     */
    private class MessageReceiver implements Runnable {
        public void run() {
            String fromServer;
            try {
                while ((fromServer = in.readLine()) != null && !sbye) {
                    appendToChat(fromServer + "\n");
                    
                    if (fromServer.equals("Bye.")) {
                        sbye = true;
                        appendToChat("服务器已断开连接\n");
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                statusLabel.setText("已断开连接");
                                inputField.setEnabled(false);
                                sendButton.setEnabled(false);
                                // 3秒后自动关闭窗口
                                new Thread(new Runnable() {
                                    public void run() {
                                        try {
                                            Thread.sleep(3000);
                                            dispose();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }).start();
                            }
                        });
                    }
                }
            } catch (IOException ex) {
                if (connected) {
                    appendToChat("连接错误: " + ex.getMessage() + "\n");
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            statusLabel.setText("连接已断开");
                            inputField.setEnabled(false);
                            sendButton.setEnabled(false);
                        }
                    });
                }
            }
            closeConnection();
        }
    }
    
    /**
     * 主方法，程序入口
     */
    public static void main(String[] args) {
        // 在事件调度线程中创建并显示GUI
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SingleTalkClient().setVisible(true);
            }
        });
    }
}