@echo off
chcp 65001 >nul
title 增强版聊天系统启动器

echo ========================================
echo       增强版聊天系统启动器
echo ========================================
echo.

echo 正在编译Java文件...
javac EnhancedMultiTalkServer.java
javac EnhancedChatClient.java
javac FileTransferServer.java
javac GroupChatManager.java

if errorlevel 1 (
    echo 编译失败，请检查Java环境配置
    pause
    exit /b 1
)

echo 编译成功！
echo.

echo 请选择要启动的服务：
echo 1. 启动聊天服务器
echo 2. 启动文件传输服务器
echo 3. 启动客户端
echo 4. 启动所有服务
echo 5. 退出
echo.

set /p choice=请输入选择 (1-5): 

if "%choice%"=="1" goto start_chat_server
if "%choice%"=="2" goto start_file_server
if "%choice%"=="3" goto start_client
if "%choice%"=="4" goto start_all
if "%choice%"=="5" goto exit

echo 无效选择！
pause
exit /b 1

:start_chat_server
echo 正在启动聊天服务器...
start "聊天服务器" java EnhancedMultiTalkServer
goto exit

:start_file_server
echo 正在启动文件传输服务器...
start "文件传输服务器" java FileTransferServer
goto exit

:start_client
echo 正在启动客户端...
start "聊天客户端" java EnhancedChatClient
goto exit

:start_all
echo 正在启动所有服务...
start "聊天服务器" java EnhancedMultiTalkServer
timeout /t 2 >nul
start "文件传输服务器" java FileTransferServer
timeout /t 2 >nul
start "聊天客户端1" java EnhancedChatClient
timeout /t 1 >nul
start "聊天客户端2" java EnhancedChatClient
echo 所有服务已启动！
goto exit

:exit
echo.
echo 启动完成！
echo 注意：请确保Java环境已正确配置
pause