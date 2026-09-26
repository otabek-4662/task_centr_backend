# Render uchun Docker build - Java 17 + Maven
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Pom ni avval ko'chirib dependency larni cache qilamiz (tez build)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Kodni ko'chirib build
COPY src ./src
COPY mvnw ./mvnw
COPY .mvn ./.mvn
RUN chmod +x mvnw
RUN mvn package -DskipTests -B

# CDS (Class Data Sharing) uchun fat-jar ni ochamiz: CDS faqat oddiy jar lardan ishlaydi,
# Spring Boot ning "jar ichida jar" formatidan emas. Klasslarimiz -> classes.jar, kutubxonalar -> lib/
RUN mkdir /extract && cd /extract && jar xf /app/target/backend-0.0.1-SNAPSHOT.jar \
 && mkdir -p /out/lib && mv BOOT-INF/lib/*.jar /out/lib/ \
 && jar cf /out/classes.jar -C BOOT-INF/classes .

# Runtime image - kichik va xavfsiz
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /out/ ./

# Render PORT ni beradi (10000), Spring ${PORT:8080} bilan o'qiydi
EXPOSE 8080

# Healthcheck uchun curl qo'shamiz
RUN apk add --no-cache curl

# Render free: 0.1 CPU / 512 MB. Bunday zaif mashinada JVM ning o'zi CPU ni "yeb qo'yadi":
#  -XX:TieredStopAtLevel=1  faqat tezkor C1 kompilyator (og'ir C2 ishga tushishni sekinlashtiradi)
#  -XX:+UseSerialGC         bitta oqimli GC — 1 yadrodan kam CPU uchun eng yengili
#  -Xss512k                 har bir thread stack i kichikroq (xotira tejash)
#  -XX:MaxRAMPercentage=75  heap konteyner xotirasining 75% gacha (512 MB dan ~384 MB)
ENV JAVA_FLAGS="-XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Xss512k -XX:MaxRAMPercentage=75"

# CDS "mashq" ishga tushirish: build paytida ilovani bir marta Spring context tayyor bo'lguncha
# ishga tushiramiz (spring.context.exit=onRefresh) va JVM yuklangan klasslarni app.jsa arxiviga yozadi.
# Baza kerak emas: Flyway o'chiq, Hibernate DB ga ulanmaydi. JWT_SECRET faqat shu buyruq uchun (image da qolmaydi).
# Classpath tartibi mashq va haqiqiy ishga tushishda bir xil bo'lishi shart — shuning uchun cp.args fayli.
RUN echo "-cp classes.jar:$(ls lib/*.jar | sort | tr '\n' ':')" > cp.args \
 && JWT_SECRET=cds-training-only-not-a-real-secret-0123456789 \
    java $JAVA_FLAGS @cp.args -XX:ArchiveClassesAtExit=app.jsa \
      -Dspring.context.exit=onRefresh \
      -Dspring.flyway.enabled=false \
      -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false \
      com.taskcenter.BackendApplication > /tmp/cds.log 2>&1 \
 || (echo "CDS mashqi muvaffaqiyatsiz — ilova CDS siz (sekinroq) ishlaydi:"; tail -20 /tmp/cds.log)

# O'lchov (0.1 CPU, 512 MB): ishga tushish ~250s -> flaglar bilan ~115s -> flaglar + CDS bilan ~52s
ENTRYPOINT ["sh", "-c", "exec java $JAVA_FLAGS -XX:SharedArchiveFile=app.jsa @cp.args com.taskcenter.BackendApplication"]
