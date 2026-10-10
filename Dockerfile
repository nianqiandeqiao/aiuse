# ---------- 1. 构建前端 ----------
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --registry=https://registry.npmmirror.com
COPY frontend/ ./
# vite.config.js 里 outDir 指向 ../backend/src/main/resources/static
RUN npm run build

# ---------- 2. 构建后端（把前端产物一起打进 jar） ----------
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /app
COPY backend/pom.xml ./backend/pom.xml
RUN mvn -f backend/pom.xml -B -q dependency:go-offline
COPY backend/src ./backend/src
COPY --from=frontend /app/backend/src/main/resources/static ./backend/src/main/resources/static
RUN mvn -f backend/pom.xml -B clean package -DskipTests

# ---------- 3. 运行 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=backend /app/backend/target/med-reminder-backend-*.jar app.jar
# 云平台会注入 PORT；本地默认 8081
ENV PORT=8081
EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java -jar app.jar --server.port=${PORT}"]

