import java.io.*;
import java.net.*;
import java.util.concurrent.*;

/**
 * 文件传输服务器
 * 支持客户端之间的文件传输
 */
public class FileTransferServer {
    private static final int FILE_PORT = 8899;
    private ServerSocket fileServerSocket;
    private ExecutorService threadPool;
    private boolean running;
    
    // 文件传输会话管理
    private ConcurrentHashMap<String, FileTransferSession> transferSessions;
    
    public FileTransferServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.transferSessions = new ConcurrentHashMap<>();
        this.running = false;
    }
    
    public void start() {
        try {
            fileServerSocket = new ServerSocket(FILE_PORT);
            running = true;
            System.out.println("文件传输服务器已启动，正在监听端口 " + FILE_PORT + "...");
            
            while (running) {
                Socket clientSocket = fileServerSocket.accept();
                threadPool.execute(new FileTransferHandler(clientSocket));
            }
            
        } catch (IOException e) {
            System.err.println("文件传输服务器启动失败: " + e.getMessage());
        } finally {
            shutdown();
        }
    }
    
    public void shutdown() {
        running = false;
        threadPool.shutdown();
        try {
            if (fileServerSocket != null && !fileServerSocket.isClosed()) {
                fileServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 文件传输处理器
     */
    class FileTransferHandler implements Runnable {
        private Socket socket;
        private DataInputStream dataIn;
        private DataOutputStream dataOut;
        private BufferedReader textIn;
        private PrintWriter textOut;
        
        public FileTransferHandler(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public void run() {
            try {
                // 初始化流
                dataIn = new DataInputStream(socket.getInputStream());
                dataOut = new DataOutputStream(socket.getOutputStream());
                textIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                textOut = new PrintWriter(socket.getOutputStream(), true);
                
                // 处理文件传输请求
                processFileTransfer();
                
            } catch (IOException e) {
                System.err.println("文件传输处理错误: " + e.getMessage());
            } finally {
                cleanup();
            }
        }
        
        private void processFileTransfer() throws IOException {
            String command = textIn.readLine();
            if (command == null) return;
            
            String[] parts = command.split(" ");
            if (parts.length < 2) {
                textOut.println("ERROR: 无效的命令格式");
                return;
            }
            
            String action = parts[0];
            String sessionId = parts[1];
            
            switch (action) {
                case "SEND_REQUEST":
                    handleSendRequest(sessionId, parts);
                    break;
                case "ACCEPT_FILE":
                    handleAcceptFile(sessionId);
                    break;
                case "REJECT_FILE":
                    handleRejectFile(sessionId);
                    break;
                case "UPLOAD_FILE":
                    handleFileUpload(sessionId);
                    break;
                case "DOWNLOAD_FILE":
                    handleFileDownload(sessionId);
                    break;
                default:
                    textOut.println("ERROR: 未知的命令");
            }
        }
        
        private void handleSendRequest(String sessionId, String[] parts) {
            if (parts.length < 5) {
                textOut.println("ERROR: 发送请求参数不足");
                return;
            }
            
            String fromUser = parts[2];
            String toUser = parts[3];
            String fileName = parts[4];
            long fileSize = Long.parseLong(parts[5]);
            
            // 创建文件传输会话
            FileTransferSession session = new FileTransferSession(
                sessionId, fromUser, toUser, fileName, fileSize);
            transferSessions.put(sessionId, session);
            
            textOut.println("SEND_REQUEST_ACK: " + sessionId);
            System.out.println("文件传输请求: " + fromUser + " -> " + toUser + " [" + fileName + "]");
        }
        
        private void handleAcceptFile(String sessionId) {
            FileTransferSession session = transferSessions.get(sessionId);
            if (session != null) {
                session.setStatus("ACCEPTED");
                textOut.println("ACCEPT_ACK: " + sessionId);
                System.out.println("文件传输已接受: " + sessionId);
            } else {
                textOut.println("ERROR: 会话不存在");
            }
        }
        
        private void handleRejectFile(String sessionId) {
            FileTransferSession session = transferSessions.get(sessionId);
            if (session != null) {
                session.setStatus("REJECTED");
                transferSessions.remove(sessionId);
                textOut.println("REJECT_ACK: " + sessionId);
                System.out.println("文件传输已拒绝: " + sessionId);
            } else {
                textOut.println("ERROR: 会话不存在");
            }
        }
        
        private void handleFileUpload(String sessionId) throws IOException {
            FileTransferSession session = transferSessions.get(sessionId);
            if (session == null || !"ACCEPTED".equals(session.getStatus())) {
                textOut.println("ERROR: 会话无效或未接受");
                return;
            }
            
            // 创建文件保存目录
            File uploadDir = new File("uploads");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            File file = new File(uploadDir, session.getFileName());
            FileOutputStream fileOut = new FileOutputStream(file);
            
            textOut.println("READY_FOR_UPLOAD");
            
            // 接收文件数据
            byte[] buffer = new byte[4096];
            long bytesReceived = 0;
            long fileSize = session.getFileSize();
            
            while (bytesReceived < fileSize) {
                int bytesRead = dataIn.read(buffer);
                if (bytesRead == -1) break;
                fileOut.write(buffer, 0, bytesRead);
                bytesReceived += bytesRead;
                
                // 显示进度
                int progress = (int) ((bytesReceived * 100) / fileSize);
                System.out.println("文件上传进度: " + progress + "%");
            }
            
            fileOut.close();
            session.setStatus("COMPLETED");
            textOut.println("UPLOAD_COMPLETE: " + sessionId);
            System.out.println("文件上传完成: " + session.getFileName());
        }
        
        private void handleFileDownload(String sessionId) throws IOException {
            FileTransferSession session = transferSessions.get(sessionId);
            if (session == null || !"ACCEPTED".equals(session.getStatus())) {
                textOut.println("ERROR: 会话无效或未接受");
                return;
            }
            
            File file = new File("uploads", session.getFileName());
            if (!file.exists()) {
                textOut.println("ERROR: 文件不存在");
                return;
            }
            
            textOut.println("READY_FOR_DOWNLOAD");
            
            FileInputStream fileIn = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                dataOut.write(buffer, 0, bytesRead);
            }
            
            fileIn.close();
            session.setStatus("COMPLETED");
            textOut.println("DOWNLOAD_COMPLETE: " + sessionId);
            System.out.println("文件下载完成: " + session.getFileName());
        }
        
        private void cleanup() {
            try {
                if (dataIn != null) dataIn.close();
                if (dataOut != null) dataOut.close();
                if (textIn != null) textIn.close();
                if (textOut != null) textOut.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 文件传输会话类
     */
    static class FileTransferSession {
        private String sessionId;
        private String fromUser;
        private String toUser;
        private String fileName;
        private long fileSize;
        private String status;
        private long startTime;
        
        public FileTransferSession(String sessionId, String fromUser, String toUser, 
                                 String fileName, long fileSize) {
            this.sessionId = sessionId;
            this.fromUser = fromUser;
            this.toUser = toUser;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.status = "PENDING";
            this.startTime = System.currentTimeMillis();
        }
        
        // Getter 和 Setter 方法
        public String getSessionId() { return sessionId; }
        public String getFromUser() { return fromUser; }
        public String getToUser() { return toUser; }
        public String getFileName() { return fileName; }
        public long getFileSize() { return fileSize; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getStartTime() { return startTime; }
    }
    
    public static void main(String[] args) {
        FileTransferServer fileServer = new FileTransferServer();
        fileServer.start();
    }
}