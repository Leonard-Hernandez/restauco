FROM amazoncorretto:25-alpine AS builder

WORKDIR /app/restauco

COPY gradle ./gradle
COPY gradlew .
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY ./src ./src

RUN ./gradlew clean bootJar -x test --no-daemon

FROM eclipse-temurin:25-jre-alpine

RUN addgroup -S restauco && adduser -S -G restauco -H -s /sbin/nologin restauco \
    && mkdir -p /data/uploads && chown restauco:restauco /data/uploads

WORKDIR /app

COPY --from=builder --chown=restauco:restauco /app/restauco/build/libs/app.jar .

USER restauco

# Dish photos (mount a volume here)
VOLUME /data/uploads

EXPOSE 8089

ENTRYPOINT [ "java", "-jar", "app.jar" ]
