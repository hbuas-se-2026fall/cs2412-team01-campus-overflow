# 多阶段构建：第一阶段编译（利用依赖缓存层），第二阶段只保留 JRE 与 jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY backend/pom.xml ./pom.xml
COPY backend/ ./
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app && apk add --no-cache wget
WORKDIR /app
# jar 的名字由 co-bootstrap/pom.xml 的 <finalName>campus-overflow</finalName> 决定，
# 不是 Maven 默认的 co-bootstrap-<version>.jar。写成 co-bootstrap-*.jar 会匹配不到，
# 镜像里就没有可运行的 jar，容器起来只会报 "Unable to access jarfile app.jar"。
COPY --from=build /src/co-bootstrap/target/campus-overflow.jar app.jar
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -Duser.timezone=UTC"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
