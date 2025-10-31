# AI聊天客户端使用说明

## 概述

AITalkClient是一个基于Java的AI聊天客户端，它连接到MultiTalkServer服务器，并使用OLLAMA的deepseek-r1:1.5b模型自动参与聊天对话。

## 功能特性

- ✅ 连接到MultiTalkServer服务器（端口8898）
- ✅ 使用OLLAMA的HTTP API与deepseek-r1:1.5b模型通信
- ✅ 自动响应其他客户端的消息
- ✅ 异步处理AI回复，模拟人类思考时间
- ✅ 资源自动管理和清理

## 系统要求

### 必需软件
1. **Java 11或更高版本** (需要java.net.http包)
2. **OLLAMA** - 本地AI模型运行环境
3. **deepseek-r1:1.5b模型** - AI对话模型

### 安装步骤

#### 1. 安装OLLAMA
**Windows:**
```bash
# 下载并安装OLLAMA
# 访问: https://ollama.ai/download
# 或使用命令行安装:
curl -fsSL https://ollama.ai/install.sh | sh
```

#### 2. 下载AI模型
```bash
# 下载deepseek-r1:1.5b模型
ollama pull deepseek-r1:1.5b
```

#### 3. 启动OLLAMA服务
```bash
# 启动OLLAMA服务（重要！）
ollama serve
```

#### 4. 测试OLLAMA
```bash
# 测试模型是否正常工作
ollama run deepseek-r1:1.5b
# 输入一些测试文本，确认模型能正常响应
```

## 使用方法

### 快速启动

1. **启动OLLAMA服务** (首先运行):
   ```bash
   ollama serve
   ```

2. **启动服务器**:
   ```bash
   # 方式1: 使用run.bat
   run.bat
   
   # 方式2: 使用start.bat  
   start.bat
   
   # 方式3: 手动编译运行
   javac MultiTalkServer.java
   java MultiTalkServer
   ```

3. **启动AI客户端**:
   ```bash
   # 方式1: 使用批处理文件（推荐）
   run_ai_client.bat
   
   # 方式2: 手动运行
   java AITalkClient
   ```

4. **启动普通客户端** (可选):
   ```bash
   javac SingleTalkClient.java
   java SingleTalkClient
   ```

### 运行流程

1. OLLAMA服务在localhost:11434运行
2. 服务器启动后监听8898端口
3. AI客户端连接到服务器并使用HTTP API与OLLAMA通信
4. 普通客户端连接到服务器
5. 当普通客户端发送消息时，AI客户端会自动生成回复
6. 所有消息都会在服务器端广播给所有连接的客户端

## 技术细节

### HTTP API通信

新的AI客户端使用OLLAMA的HTTP API而不是进程通信：
- **API端点**: `http://localhost:11434/api/generate`
- **请求方法**: POST
- **数据格式**: JSON
- **超时设置**: 30秒

### 消息处理机制

- **@AI标记**: AI客户端只回复包含"@AI"标记的消息
- **消息过滤**: AI客户端会过滤掉自己发送的消息和服务器广播消息
- **异步响应**: 使用线程池异步生成AI回复，避免阻塞主线程
- **思考时间**: 模拟人类思考，延迟1-3秒后发送回复
- **消息解析**: 从服务器消息格式中提取实际内容

### 错误处理

- 网络连接失败时会显示错误信息
- OLLAMA服务未启动时会提示启动命令
- HTTP API错误会显示状态码和错误信息
- 自动资源清理，确保连接正确关闭

## 故障排除

### 常见问题

1. **连接服务器失败**
   - 检查服务器是否正在运行
   - 确认服务器监听8898端口
   - 检查防火墙设置

2. **OLLAMA服务未启动**
   - 确认OLLAMA服务正在运行: `ollama serve`
   - 检查11434端口是否被占用
   - 查看OLLAMA日志输出

3. **AI不回复消息**
   - 检查OLLAMA服务是否正常运行
   - 查看AI客户端控制台输出是否有错误信息
   - 确认deepseek-r1:1.5b模型已下载
   - 运行 `ollama list` 查看已安装的模型

4. **Java版本问题**
   - 确认使用Java 11或更高版本
   - 检查java.net.http包是否可用

### 调试技巧

1. 查看服务器控制台输出
2. 检查AI客户端控制台日志
3. 使用 `ollama run deepseek-r1:1.5b` 单独测试模型
4. 查看chat.log文件中的聊天记录
5. 检查OLLAMA服务是否在11434端口监听

## 文件说明

- `AITalkClient.java` - AI客户端主程序（使用HTTP API）
- `run_ai_client.bat` - Windows批处理启动脚本
- `test_ai_client.bat` - 编译测试脚本
- `MultiTalkServer.java` - 多线程聊天服务器
- `SingleTalkClient.java` - 普通聊天客户端
- `chat.log` - 聊天记录日志文件

## 扩展建议

1. **模型选择**: 可以替换为其他OLLAMA支持的模型
2. **回复策略**: 可以自定义AI的回复风格和策略
3. **多AI支持**: 可以创建多个AI客户端使用不同模型
4. **Web界面**: 可以为AI客户端添加Web界面

## 注意事项

- 确保OLLAMA服务在启动AI客户端前已运行
- 确保有足够的系统资源运行OLLAMA模型
- 首次运行可能需要下载模型，请耐心等待
- 建议在局域网环境中测试
- 生产环境使用时考虑安全性和性能优化

## 版本更新

### v2.0 更新内容
- 使用HTTP API替代进程通信，提高稳定性
- 支持Java 11+的java.net.http包
- 改进错误处理和用户提示
- 简化模型交互逻辑