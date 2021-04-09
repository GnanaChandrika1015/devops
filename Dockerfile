FROM registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift
MAINTAINER "IBM BAT Applciation Team"
COPY target/dispatch-msg-ingest-service-1.0.0.RELEASE.jar /opt/lib/
ENV JAVA_OPTS="" LANG='en_US.UTF-8'
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /opt/lib/dispatch-msg-ingest-service-1.0.0.RELEASE.jar" ]
EXPOSE 8080
