FROM gcr.io/distroless/java17
COPY target/storagemanager*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar","-Xmx=512M"]