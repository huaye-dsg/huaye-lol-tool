#!/bin/bash

# LOL工具启动脚本
# 支持两种模式：pure-spring（纯Spring控制台模式）和 springboot（SpringBoot Web模式，默认）

# 检查参数
MODE=${1:-springboot}  # 默认SpringBoot模式

echo "🚀 启动LOL工具后端服务..."
cd huaye-lol-tool-server

case $MODE in
    "pure-spring"|"console")
        echo "📱 使用纯Spring模式启动（仅控制台，无Web服务器）..."
        mvn spring-boot:run -Dspring-boot.run.arguments="--pure-spring" &
        BACKEND_PID=$!
        echo "✅ 纯Spring模式启动完成！"
        echo "📋 LOL客户端监控服务已启动"
        echo "🔧 按 Ctrl+C 退出程序"
        ;;
    "springboot"|"web")
        echo "🌐 使用SpringBoot Web模式启动（包含Web服务器）..."
        mvn spring-boot:run &
        BACKEND_PID=$!

        # 等待后端启动完成（等待9527端口可用）
        echo "⏳ 等待后端服务启动..."
        while ! nc -z localhost 9527 2>/dev/null; do
          sleep 1
        done
        echo "✅ SpringBoot Web服务启动完成！"
        echo "🌐 Web服务器地址: http://localhost:9527"
        echo "📱 LOL客户端监控服务已启动"

        # 启动前端开发服务器
        echo "🎨 启动前端开发服务器..."
        cd ../huaye-lol-tool-web
        pnpm install
        pnpm dev
        ;;
    *)
        echo "❌ 未知模式: $MODE"
        echo "📖 支持的模式:"
        echo "   pure-spring 或 console  - 纯Spring控制台模式"
        echo "   springboot 或 web       - SpringBoot Web模式（默认）"
        echo ""
        echo "📝 使用示例:"
        echo "   ./start.sh pure-spring   # 启动纯Spring模式"
        echo "   ./start.sh springboot    # 启动SpringBoot Web模式"
        echo "   ./start.sh               # 默认启动SpringBoot Web模式"
        exit 1
        ;;
esac

# 添加优雅退出处理
trap 'echo "🔄 正在关闭服务..."; kill $BACKEND_PID 2>/dev/null; exit 0' INT TERM
wait