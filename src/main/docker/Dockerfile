FROM frolvlad/alpine-oraclejdk8:slim

VOLUME /tmp

ADD temp-munger.jar app.jar

RUN sh -c 'touch /app.jar'

ENV JAVA_OPTS="-Xms128m -Xmx1024m"

ENTRYPOINT ["sh", "-c"]

CMD ["java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app.jar"]
