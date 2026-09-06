# ============================================================
# SmartSlot Frontend Multi-Stage Dockerfile (Vite + Nginx)
# ============================================================

# Stage 1: 构建静态资源
FROM node:20-alpine AS builder
WORKDIR /build

# 安装依赖
COPY package*.json ./
RUN npm config set registry https://registry.npmmirror.com/ \
    && npm install

# 复制源码并构建打包
COPY . .
RUN npm run build

# Stage 2: Nginx 生产级镜像托管
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# 拷贝构建出的静态文件与 Nginx 反代配置
COPY --from=builder /build/dist .
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
