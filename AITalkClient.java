import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * AI聊天客户端类
 * 连接到MultiTalkServer服务器，使用OLLAMA的deepseek-r1:1.5b模型参与聊天
 * 作为一个AI客户端自动响应其他客户端的消息
 */
public class AITalkClient {
    // 网络相关变量
    private Socket client = null;
    private PrintWriter out = null;
    private BufferedReader in = null;
    private boolean connected = false;
    private boolean sbye = false;         // 服务器结束标志
    private boolean ubye = false;         // 用户结束标志
    
    // AI相关变量
    private final String AI_NAME = "AI助手";
    private final String OLLAMA_URL = "http://localhost:11434";
    private HttpClient httpClient;
    
    // 线程池用于处理AI响应
    private ExecutorService executor = Executors.newFixedThreadPool(2);
    
    /**
     * 构造函数，初始化网络连接和AI模型
     */
    public AITalkClient() {
        initHttpClient();
        initNetwork();
    }
    
    /**
     * 初始化HTTP客户端
     */
    private void initHttpClient() {
        httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        System.out.println("HTTP客户端已初始化");
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
            System.out.println("AI客户端已连接到服务器 " + HOST + ":8898");
            System.out.println("AI助手已上线，正在监听聊天室消息...");
            
            // 创建线程接收服务器消息
            new Thread(new MessageReceiver()).start();
        } catch (UnknownHostException ex) {
            System.err.println("错误: 未知主机 " + HOST);
        } catch (IOException ex) {
            System.err.println("错误: 无法连接到服务器 " + HOST + ":8898");
        }
    }
    
    /**
     * 生成AI回复
     * @param userMessage 用户消息
     * @return AI回复
     */
    private String generateAIResponse(String userMessage) {
        if (!connected) return null;
        
        try {
            // 构建请求JSON
            String requestBody = String.format(
                "{\"model\": \"deepseek-r1:1.5b\", " +
                "\"prompt\": \"你是一个友好的AI助手，正在参与一个多人聊天室。请用简洁、友好的方式回复用户的消息。保持对话自然流畅。用户说: %s\", " +
                "\"stream\": false}",
                userMessage.replace("\"", "\\\"")
            );
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .timeout(Duration.ofSeconds(30))
                .build();
            
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                // 解析响应JSON
                String responseBody = response.body();
                // 简单提取回复内容
                if (responseBody.contains("\"response\":")) {
                    int start = responseBody.indexOf("\"response\":\"") + 12;
                    int end = responseBody.indexOf("\"", start);
                    if (start > 11 && end > start) {
                        String aiResponse = responseBody.substring(start, end);
                        // 清理响应，移除思考内容（think与/think之间的内容）
                        aiResponse = removeThinkContent(aiResponse);
                        // 清理响应，移除可能的提示词
                        aiResponse = aiResponse.replace("请回复:", "").trim();
                        aiResponse = aiResponse.replace("用户说: " + userMessage, "").trim();
                        if (aiResponse.length() > 200) {
                            aiResponse = aiResponse.substring(0, 200) + "...";
                        }
                        return aiResponse.isEmpty() ? "我明白了，谢谢分享！" : aiResponse;
                    }
                }
            } else {
                System.err.println("OLLAMA API错误: " + response.statusCode() + " - " + response.body());
            }
            
        } catch (Exception e) {
            System.err.println("生成AI回复时出错: " + e.getMessage());
            if (e.getMessage().contains("Connection refused")) {
                System.err.println("请确保OLLAMA服务正在运行: ollama serve");
            }
        }
        
        return "这个问题很有趣，让我想想...";
    }
    
    /**
     * 发送消息到服务器
     * @param message 消息内容
     */
    private void sendMessage(String message) {
        if (!connected || ubye) return;
        
        if (message != null && !message.isEmpty()) {
            out.println(message);
            System.out.println(AI_NAME + " 发送: " + message);
        }
    }
    
    /**
     * 移除思考内容（think与/think之间的内容）
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String removeThinkContent(String text) {
        StringBuilder result = new StringBuilder();
        int startIndex = 0;
        int thinkStart;
        
        while ((thinkStart = text.indexOf("think", startIndex)) != -1) {
            // 添加think之前的内容
            result.append(text.substring(startIndex, thinkStart));
            
            // 查找think结束位置
            int thinkEnd = text.indexOf("/think", thinkStart);
            if (thinkEnd == -1) {
                // 如果没有找到/think，保留剩余内容
                startIndex = thinkStart;
                break;
            }
            
            // 跳过think内容，从/think之后继续
            startIndex = thinkEnd + 6; // 6是"/think"的长度
        }
        
        // 添加剩余内容
        if (startIndex < text.length()) {
            result.append(text.substring(startIndex));
        }
        
        return result.toString().trim();
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
            System.out.println("AI客户端已断开连接");
        } catch (IOException ex) {
            System.err.println("关闭连接时发生错误");
        }
        
        // 关闭线程池
        executor.shutdown();
    }
    
    /**
     * 消息接收线程类，用于接收服务器消息
     */
    private class MessageReceiver implements Runnable {
        public void run() {
            String fromServer;
            try {
                while ((fromServer = in.readLine()) != null && !sbye) {
                    System.out.println("收到消息: " + fromServer);
                    
                    // 检查是否是服务器断开连接的消息
                    if (fromServer.equals("Bye.")) {
                        sbye = true;
                        System.out.println("服务器已断开连接");
                        break;
                    }
                    
                    // 过滤掉AI自己发送的消息和服务器广播消息
                    if (!fromServer.contains(AI_NAME) &&
                        !fromServer.startsWith("[服务器]:") &&
                        !fromServer.contains("已加入聊天室") &&
                        !fromServer.contains("已离开聊天室")) {
                        
                        // 提取实际的消息内容
                        String messageContent = extractMessageContent(fromServer);
                        if (messageContent != null && !messageContent.isEmpty()) {
                            // 检查消息是否包含@AI标记
                            if (messageContent.contains("@AI")) {
                                // 移除@AI标记后发送给AI模型
                                String cleanMessage = messageContent.replace("@AI", "").trim();
                                if (!cleanMessage.isEmpty()) {
                                    // 使用线程池异步生成AI回复
                                    final String finalMessage = cleanMessage;
                                    executor.submit(() -> {
                                        String aiResponse = generateAIResponse(finalMessage);
                                        if (aiResponse != null && !aiResponse.isEmpty()) {
                                            // 延迟1-3秒后发送回复，模拟人类思考时间
                                            try {
                                                Thread.sleep(1000 + (long)(Math.random() * 2000));
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                            }
                                            sendMessage(aiResponse);
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                if (connected) {
                    System.err.println("连接错误: " + ex.getMessage());
                }
            }
            closeConnection();
        }
        
        /**
         * 从服务器消息中提取实际的消息内容
         * @param serverMessage 服务器消息
         * @return 提取的消息内容
         */
        private String extractMessageContent(String serverMessage) {
            // 服务器消息格式: [客户端X(IP: xxx.xxx.xxx.xxx)]: 消息内容
            int colonIndex = serverMessage.indexOf("]:");
            if (colonIndex != -1 && colonIndex + 2 < serverMessage.length()) {
                return serverMessage.substring(colonIndex + 2).trim();
            }
            return serverMessage;
        }
    }
    
    /**
     * 主方法，程序入口
     */
    public static void main(String[] args) {
        System.out.println("启动AI聊天客户端...");
        System.out.println("正在连接服务器和初始化AI模型...");
        
        AITalkClient aiClient = new AITalkClient();
        
        // 添加关闭钩子，确保资源正确释放
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n正在关闭AI客户端...");
            aiClient.closeConnection();
        }));
        
        // 保持程序运行
        try {
            while (aiClient.connected && !aiClient.sbye) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("AI客户端已退出");
    }
}