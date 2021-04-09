FROM openjdk:8
WORKDIR /app
COPY gs-maven-0.1.0.jar .
CMD java -jar gs-maven-0.1.0.jar
EXPOSE 8081
