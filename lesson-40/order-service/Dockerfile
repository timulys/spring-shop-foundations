# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy@sha256:432e54724c3e57d1846aeb886f4d339f3b0522931c4e64119c2797013b7cfc3c AS build
WORKDIR /workspace
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle,sharing=locked chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-jammy@sha256:dff6eb46527bd293a3c06bbbe9e64e8c9f91f01905b5abbdad31761c03059d2c
RUN groupadd --gid 10001 shop && useradd --uid 10001 --gid shop --no-create-home shop
WORKDIR /app
COPY --from=build --chown=shop:shop /workspace/build/libs/order-service-lesson-40-0.0.1.jar /app/shop.jar
USER shop:shop
ENV SERVER_ADDRESS=0.0.0.0
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/shop.jar"]
