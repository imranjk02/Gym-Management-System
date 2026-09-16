FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY src/java /app/src
COPY web /app/web

RUN mkdir -p /app/lib
RUN cp /app/web/WEB-INF/lib/*.jar /app/lib/

RUN mkdir -p /app/classes
RUN javac -cp "/app/lib/*" -d /app/classes /app/src/*.java

EXPOSE 10000

CMD ["sh", "-c", "java -cp '/app/classes:/app/lib/*' Main"]