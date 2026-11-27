FROM node:20-alpine AS build
WORKDIR /src
# 带上 lockfile 才能用 npm ci；两者都不存在时 npm ci 会直接失败，正好挡住漏提交
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /src/dist /usr/share/nginx/html
EXPOSE 80
