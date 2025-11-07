import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * QQ登录界面
 */
public class QQLoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    
    public QQLoginFrame() {
        initComponents();
        setupLayout();
        setupListeners();
    }
    
    private void initComponents() {
        setTitle("Java QQ - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        
        // 创建组件
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        loginButton = new JButton("登录");
        cancelButton = new JButton("取消");
        
        // 设置按钮样式
        loginButton.setBackground(new Color(0, 120, 215));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 标题
        JLabel titleLabel = new JLabel("Java QQ");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 24));
        titleLabel.setForeground(new Color(0, 120, 215));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // 表单面板
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // 用户名行
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        formPanel.add(usernameField, gbc);
        
        // 密码行
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        
        // 添加组件到主面板
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(formPanel);
        mainPanel.add(buttonPanel);
        
        add(mainPanel, BorderLayout.CENTER);
        
        pack();
        setLocationRelativeTo(null); // 居中显示
    }
    
    private void setupListeners() {
        // 登录按钮事件
        loginButton.addActionListener(e -> performLogin());
        
        // 取消按钮事件
        cancelButton.addActionListener(e -> System.exit(0));
        
        // 回车键登录
        passwordField.addActionListener(e -> performLogin());
        
        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
    
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "用户名和密码不能为空！", 
                "登录失败", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 显示登录中状态
        loginButton.setEnabled(false);
        loginButton.setText("登录中...");
        
        // 在实际应用中，这里应该连接到服务器进行认证
        // 这里我们模拟认证过程
        new Thread(() -> {
            try {
                // 模拟网络延迟
                Thread.sleep(1000);
                
                SwingUtilities.invokeLater(() -> {
                    // 简单的认证逻辑（在实际应用中应该连接到服务器）
                    if (isValidCredentials(username, password)) {
                        // 登录成功，打开主界面
                        openMainFrame(username);
                        dispose(); // 关闭登录窗口
                    } else {
                        // 登录失败
                        JOptionPane.showMessageDialog(this, 
                            "用户名或密码错误！\n\n测试账号：\nuser1/pass1\nuser2/pass2\nuser3/pass3", 
                            "登录失败", 
                            JOptionPane.ERROR_MESSAGE);
                        loginButton.setEnabled(true);
                        loginButton.setText("登录");
                        passwordField.setText("");
                        passwordField.requestFocus();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    private boolean isValidCredentials(String username, String password) {
        // 简单的认证逻辑（在实际应用中应该连接到服务器）
        return (username.equals("user1") && password.equals("pass1")) ||
               (username.equals("user2") && password.equals("pass2")) ||
               (username.equals("user3") && password.equals("pass3"));
    }
    
    private void openMainFrame(String username) {
        SwingUtilities.invokeLater(() -> {
            try {
                String password = new String(passwordField.getPassword());
                QQNetworkClient mainFrame = new QQNetworkClient(username, password);
                mainFrame.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "无法启动主界面: " + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    public static void main(String[] args) {
        // 设置系统外观
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        SwingUtilities.invokeLater(() -> {
            QQLoginFrame loginFrame = new QQLoginFrame();
            loginFrame.setVisible(true);
        });
    }
}