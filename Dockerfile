FROM registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift
MAINTAINER "IBM BAT Applciation Team"
COPY target/gs-maven-0.1.0.jar /opt/lib/
ENV JAVA_OPTS="" LANG='en_US.UTF-8'
EXPOSE 8080
