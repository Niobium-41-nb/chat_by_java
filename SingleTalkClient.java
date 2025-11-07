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