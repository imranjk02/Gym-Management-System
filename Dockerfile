FROM eclipse-temurin:17-jdk

WORKDIR /app

RUN apt-get update && apt-get install -y curl

COPY src/java /app/src
COPY web /app/web

RUN mkdir -p /app/lib /app/classes

RUN curl -L -o /app/lib/mysql-connector-j.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar

RUN javac -cp "/app/lib/*" -d /app/classes /app/src/*.java

EXPOSE 10000

CMD ["sh", "-c", "java -cp '/app/classes:/app/lib/*' Main"]