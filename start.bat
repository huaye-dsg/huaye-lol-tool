@echo off
:: LOL工具启动脚本（Windows版本）
:: 支持两种模式：pure-spring（纯Spring控制台模式）和 springboot（SpringBoot Web模式，默认）

:: 检查参数
if "%1"=="" (
    set MODE=springboot
) else (
    set MODE=%1
)

echo 🚀 启动LOL工具后端服务...
cd huaye-lol-tool-server

if "%MODE%"=="pure-spring" goto :pure_spring
if "%MODE%"=="console" goto :pure_spring
if "%MODE%"=="springboot" goto :springboot
if "%MODE%"=="web" goto :springboot

:: 默认SpringBoot模式
goto :springboot

:pure_spring
echo 📱 使用纯Spring模式启动（仅控制台，无Web服务器）...
start /B mvn spring-boot:run -Dspring-boot.run.arguments="--pure-spring"
echo ✅ 纯Spring模式启动完成！
echo 📋 LOL客户端监控服务已启动
echo 🔧 按 Ctrl+C 退出程序
goto :end

:springboot
echo 🌐 使用SpringBoot Web模式启动（包含Web服务器）...
start /B mvn spring-boot:run

:: 等待后端启动完成（等待9527端口可用）
echo ⏳ 等待后端服务启动...
:checkPort
timeout /t 1 /nobreak > nul
netstat -an | find "9527" | find "LISTENING" > nul
if errorlevel 1 goto :checkPort

echo ✅ SpringBoot Web服务启动完成！
echo 🌐 Web服务器地址: http://localhost:9527
echo 📱 LOL客户端监控服务已启动

:: 启动前端开发服务器
echo 🎨 启动前端开发服务器...
cd ../huaye-lol-tool-web
call pnpm install
call pnpm dev
goto :end

:help
echo ❌ 未知模式: %MODE%
echo 📖 支持的模式:
echo    pure-spring 或 console  - 纯Spring控制台模式
echo    springboot 或 web       - SpringBoot Web模式（默认）
echo.
echo 📝 使用示例:
echo    start.bat pure-spring   # 启动纯Spring模式
echo    start.bat springboot    # 启动SpringBoot Web模式
echo    start.bat               # 默认启动SpringBoot Web模式
pause
exit /b 1

:end