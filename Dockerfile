FROM eclipse-temurin:25
COPY target/dukeops-*.jar /usr/app/app.jar
RUN groupadd -g 1001 dukeops && useradd -m -u 1001 -g 1001 dukeops
USER dukeops
EXPOSE 8080
CMD ["java", "-jar", "/usr/app/app.jar"]
HEALTHCHECK CMD curl --fail --silent localhost:8080/actuator/health | grep UP || exit 1
