#!/bin/bash

# 启动后端服务
echo "Starting backend server..."
cd huaye-lol-tool-server
mvn spring-boot:run &

# 等待后端启动完成（等待8080端口可用）
while ! nc -z localhost 9527; do
  sleep 1
done
echo "Backend server is ready!"

# 启动前端开发服务器
echo "Starting frontend dev server..."
cd ../huaye-lol-tool-web
pnpm install
pnpm dev