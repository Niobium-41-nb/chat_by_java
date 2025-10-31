import java.util.*;
import java.util.concurrent.*;

/**
 * 群聊管理器
 * 管理多个聊天群组，支持群组创建、加入、退出和消息广播
 */
public class GroupChatManager {
    private static GroupChatManager instance;
    private Map<String, ChatGroup> groups;
    private Map<String, Set<String>> userGroups; // 用户加入的群组
    
    private GroupChatManager() {
        this.groups = new ConcurrentHashMap<>();
        this.userGroups = new ConcurrentHashMap<>();
        initializeDefaultGroups();
    }
    
    public static synchronized GroupChatManager getInstance() {
        if (instance == null) {
            instance = new GroupChatManager();
        }
        return instance;
    }
    
    /**
     * 初始化默认群组
     */
    private void initializeDefaultGroups() {
        createGroup("技术交流", "讨论编程和技术问题");
        createGroup("休闲聊天", "日常闲聊和娱乐话题");
        createGroup("学习小组", "学习交流和资源共享");
        createGroup("游戏天地", "游戏讨论和组队");
    }
    
    /**
     * 创建新群组
     */
    public boolean createGroup(String groupName, String description) {
        if (groups.containsKey(groupName)) {
            return false; // 群组已存在
        }
        
        ChatGroup group = new ChatGroup(groupName, description);
        groups.put(groupName, group);
        System.out.println("创建群组: " + groupName + " - " + description);
        return true;
    }
    
    /**
     * 用户加入群组
     */
    public boolean joinGroup(String username, String groupName) {
        ChatGroup group = groups.get(groupName);
        if (group == null) {
            return false; // 群组不存在
        }
        
        if (group.addMember(username)) {
            // 更新用户的群组列表
            userGroups.computeIfAbsent(username, k -> new HashSet<>()).add(groupName);
            System.out.println("用户 " + username + " 加入群组 " + groupName);
            return true;
        }
        return false;
    }
    
    /**
     * 用户退出群组
     */
    public boolean leaveGroup(String username, String groupName) {
        ChatGroup group = groups.get(groupName);
        if (group == null) {
            return false; // 群组不存在
        }
        
        if (group.removeMember(username)) {
            // 更新用户的群组列表
            Set<String> userGroupSet = userGroups.get(username);
            if (userGroupSet != null) {
                userGroupSet.remove(groupName);
                if (userGroupSet.isEmpty()) {
                    userGroups.remove(username);
                }
            }
            System.out.println("用户 " + username + " 退出群组 " + groupName);
            return true;
        }
        return false;
    }
    
    /**
     * 发送群组消息
     */
    public void sendGroupMessage(String username, String groupName, String message) {
        ChatGroup group = groups.get(groupName);
        if (group == null || !group.hasMember(username)) {
            return; // 群组不存在或用户不在群组中
        }
        
        String formattedMessage = "[" + groupName + "] " + username + ": " + message;
        group.broadcastMessage(formattedMessage, username);
        System.out.println("群组消息 [" + groupName + "]: " + username + " - " + message);
    }
    
    /**
     * 获取用户加入的所有群组
     */
    public Set<String> getUserGroups(String username) {
        return userGroups.getOrDefault(username, Collections.emptySet());
    }
    
    /**
     * 获取所有群组信息
     */
    public List<ChatGroup> getAllGroups() {
        return new ArrayList<>(groups.values());
    }
    
    /**
     * 获取群组成员列表
     */
    public Set<String> getGroupMembers(String groupName) {
        ChatGroup group = groups.get(groupName);
        return group != null ? group.getMembers() : Collections.emptySet();
    }
    
    /**
     * 删除群组（仅管理员）
     */
    public boolean deleteGroup(String groupName) {
        ChatGroup removedGroup = groups.remove(groupName);
        if (removedGroup != null) {
            // 从所有用户的群组列表中移除该群组
            for (Set<String> userGroupSet : userGroups.values()) {
                userGroupSet.remove(groupName);
            }
            System.out.println("删除群组: " + groupName);
            return true;
        }
        return false;
    }
    
    /**
     * 聊天群组类
     */
    public static class ChatGroup {
        private String name;
        private String description;
        private Set<String> members;
        private String creator;
        private long createTime;
        
        public ChatGroup(String name, String description) {
            this.name = name;
            this.description = description;
            this.members = ConcurrentHashMap.newKeySet();
            this.creator = "System";
            this.createTime = System.currentTimeMillis();
        }
        
        public ChatGroup(String name, String description, String creator) {
            this.name = name;
            this.description = description;
            this.members = ConcurrentHashMap.newKeySet();
            this.creator = creator;
            this.createTime = System.currentTimeMillis();
            this.members.add(creator); // 创建者自动加入
        }
        
        /**
         * 添加成员
         */
        public boolean addMember(String username) {
            return members.add(username);
        }
        
        /**
         * 移除成员
         */
        public boolean removeMember(String username) {
            return members.remove(username);
        }
        
        /**
         * 检查用户是否在群组中
         */
        public boolean hasMember(String username) {
            return members.contains(username);
        }
        
        /**
         * 广播消息给所有成员（除了发送者）
         */
        public void broadcastMessage(String message, String excludeUser) {
            // 在实际实现中，这里应该调用客户端的消息发送方法
            // 这里只是模拟广播逻辑
            for (String member : members) {
                if (!member.equals(excludeUser)) {
                    // 在实际系统中，这里应该发送消息给对应的客户端
                    System.out.println("发送给 " + member + ": " + message);
                }
            }
        }
        
        /**
         * 获取群组信息
         */
        public String getGroupInfo() {
            return String.format("群组: %s\n描述: %s\n创建者: %s\n成员数: %d\n创建时间: %s",
                    name, description, creator, members.size(), 
                    new Date(createTime).toString());
        }
        
        // Getter 方法
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Set<String> getMembers() { return new HashSet<>(members); }
        public String getCreator() { return creator; }
        public long getCreateTime() { return createTime; }
        public int getMemberCount() { return members.size(); }
    }
    
    /**
     * 群组消息类
     */
    public static class GroupMessage {
        private String groupName;
        private String sender;
        private String content;
        private long timestamp;
        
        public GroupMessage(String groupName, String sender, String content) {
            this.groupName = groupName;
            this.sender = sender;
            this.content = content;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getFormattedMessage() {
            return String.format("[%s] %s (%s): %s", 
                    groupName, sender, 
                    new Date(timestamp).toString(), content);
        }
        
        // Getter 方法
        public String getGroupName() { return groupName; }
        public String getSender() { return sender; }
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        GroupChatManager manager = GroupChatManager.getInstance();
        
        // 测试群组功能
        System.out.println("=== 群聊管理器测试 ===");
        
        // 显示所有群组
        System.out.println("\n所有群组:");
        for (ChatGroup group : manager.getAllGroups()) {
            System.out.println("- " + group.getName() + ": " + group.getDescription());
        }
        
        // 测试用户加入群组
        manager.joinGroup("user1", "技术交流");
        manager.joinGroup("user1", "休闲聊天");
        manager.joinGroup("user2", "技术交流");
        manager.joinGroup("user3", "休闲聊天");
        
        // 测试发送群组消息
        manager.sendGroupMessage("user1", "技术交流", "大家好，我是新来的！");
        manager.sendGroupMessage("user2", "技术交流", "欢迎新人！");
        manager.sendGroupMessage("user3", "休闲聊天", "今天天气真好！");
        
        // 显示用户加入的群组
        System.out.println("\nuser1 加入的群组:");
        for (String group : manager.getUserGroups("user1")) {
            System.out.println("- " + group);
        }
        
        // 显示群组成员
        System.out.println("\n技术交流群组成员:");
        for (String member : manager.getGroupMembers("技术交流")) {
            System.out.println("- " + member);
        }
    }
}