FROM openjdk:8
LABEL maintainer="gnana.chandrika.myla@ibm.com" 
COPY /target/gs-maven-0.1.0.jar /home/gs-maven-0.1.0.jar
CMD ["java","-jar","-Dspring.profiles.active=default","/home/gs-maven-0.1.0.jar"]
EXPOSE 8081
