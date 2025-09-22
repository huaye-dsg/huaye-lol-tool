# Huaye LOL Tool

一个英雄联盟赛前助手工具，包含前后端项目。

## 项目结构

- `huaye-lol-tool-web/`: Vue 前端项目
  - 提供用户界面
  - 实现功能可视化操作

- `huaye-lol-tool-server/`: SpringBoot 后端项目
  - 通过 LCU 接口实现赛前功能
  - 提供 RESTful API 接口

## 主要功能

1. **自动选择英雄**
   - 自动选择指定英雄✅
   - 根据玩家的胜率和使用习惯，自动选择推荐的英雄
   - 根据敌我双方阵容自动选择英雄

2. **自动禁用英雄**
   - 自动禁用指定英雄✅
   - 提供禁用列表，如果有队友预选，则依次顺延禁用

3. **查询队友战绩**
   - 解析上等马还是下等马 ✅
   - 分析队友常用位置和补位情况
   - 实时分析队友选择英雄情况✅

4. **其他功能**
   - 英雄选择统计
   - 最佳搭配推荐
   - 角色适应性建议
   - 定位分析
   - 个人对线打法建议

## 开发指南

### 后端开发
1. 进入后端目录：`cd huaye-lol-tool-server`
2. 安装依赖：`mvn install`
3. 运行项目：`mvn spring-boot:run`

### 前端开发
1. 进入前端目录：`cd huaye-lol-tool-web`
2. 安装依赖：`pnpm install`
3. 运行开发服务器：`pnpm dev`

## 构建部署

### 后端打包
```bash
cd huaye-lol-tool-server
mvn clean package
```

### 前端打包
```bash
cd huaye-lol-tool-web
pnpm build
```

## 注意事项
- 确保本地已安装 Java、Node.js 环境
- 前端开发推荐使用 Node.js LTS 版本
- 需要正确配置 LCU 接口相关参数
