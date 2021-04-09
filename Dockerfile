FROM java:8
WORKDIR /
ADD gs-maven-0.1.0.jar gs-maven-0.1.0.jar
EXPOSE 8081
CMD java - jar gs-maven-0.1.0.jar
