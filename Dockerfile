FROM dvagcr.azurecr.io/bp/openjdk:11

VOLUME /tmp

ENV JAVA_TOOL_OPTIONS=""
ENV SPRING_PROFILES_ACTIVE="default"
EXPOSE 8080
EXPOSE 1099

COPY target/dependency /lib
ADD target/springbootonkubernetes.jar app.jar
