import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class SingleTalkClient extends JFrame {
    private Socket client = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private boolean connected = false;
    
    private JTextArea messageArea; 
    private JTextField inputField; 
    private JButton sendButton;   
    private JTextField serverField; 
    private JTextField portField;   
    private JButton connectButton;  

    public SingleTalkClient() {
        super("聊天客户端");
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
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
        
        messageArea = new JTextArea();
        messageArea.setEditable(false); 
        JScrollPane scrollPane = new JScrollPane(messageArea);
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        sendButton = new JButton("发送");
        sendButton.setEnabled(false); 
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(connectPanel, BorderLayout.NORTH);
        contentPane.add(scrollPane, BorderLayout.CENTER);
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
            serverField.setEnabled(true);
            portField.setEnabled(true);
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
            appendMessage("我: " + message);
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
                                appendMessage("服务器: " + message);
                                
                                if (message.equals("Bye.")) {
                                    disconnectFromServer();
                                }
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