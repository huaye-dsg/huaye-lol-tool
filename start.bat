@echo off
:: 启动后端服务
echo Starting backend server...
cd huaye-lol-tool-dev
start /B mvn spring-boot:run

:: 等待后端启动完成（等待9527端口可用）
:checkPort
timeout /t 1 /nobreak > nul
netstat -an | find "9527" | find "LISTENING" > nul
if errorlevel 1 goto :checkPort
echo Backend server is ready!

:: 启动前端开发服务器
echo Starting frontend dev server...
cd ../huaye-lol-tool-web
call pnpm install
call pnpm dev