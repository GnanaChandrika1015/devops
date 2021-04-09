FROM java:8
WORKDIR /
ADD gs-maven-0.1.0.jar
EXPOSE 8080
CMD java - jar gs-maven-0.1.0.jar
