FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /workspace

COPY ToneAnalysisServer.java .
RUN javac ToneAnalysisServer.java

# создание уменьшенной JRE
RUN jlink \
    --module-path /opt/java/openjdk/jmods \
    --add-modules java.base,jdk.httpserver \
    --output /opt/custom-jre \
    --strip-debug \
    --no-header-files \
    --no-man-pages

# === runtime ===
FROM alpine:3.20

WORKDIR /app

COPY --from=builder /opt/custom-jre /opt/custom-jre
COPY --from=builder /workspace/ToneAnalysisServer.class .

ENV PATH="/opt/custom-jre/bin:${PATH}"

CMD ["java", "ToneAnalysisServer"]
