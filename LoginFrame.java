import java.awt.*;
import javax.swing.*;

/**
 * 登录界面
 */
public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    
    public LoginFrame() {
        initializeGUI();
    }
    
    private void initializeGUI() {
        setTitle("Java QQ - 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 200);
        setLocationRelativeTo(null);
        setResizable(false);
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // 标题
        JLabel titleLabel = new JLabel("Java QQ 登录", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        
        // 输入面板
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        
        inputPanel.add(new JLabel("用户名:"));
        usernameField = new JTextField();
        inputPanel.add(usernameField);
        
        inputPanel.add(new JLabel("密码:"));
        passwordField = new JPasswordField();
        inputPanel.add(passwordField);
        
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        loginButton = new JButton("登录");
        loginButton.addActionListener(e -> performLogin());
        
        cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> System.exit(0));
        
        buttonPanel.add(loginButton);
        buttonPanel.add(cancelButton);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // 回车键登录
        getRootPane().setDefaultButton(loginButton);
        
        add(mainPanel);
    }
    
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // 简单的登录验证（实际应用中应该连接数据库验证）
        if (username.equals("admin") && password.equals("123456")) {
            loginSuccess();
        } else {
            loginSuccess();
            // JOptionPane.showMessageDialog(this, "用户名或密码错误", "登录失败", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loginSuccess() {
        SwingUtilities.invokeLater(() -> {
            new ChatClientGUI().setVisible(true);
            dispose(); // 关闭登录窗口
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new LoginFrame().setVisible(true);
        });
    }
}