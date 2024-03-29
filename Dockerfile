FROM eclipse-temurin:17-alpine
RUN apk update && apk upgrade && apk add curl && rm -rf /var/cache/apk/* \
    && mkdir -p /opt/dmda/logs/ \
    && adduser -s /bin/bash -u 6000 -D -H dmda \
    && chown -R dmda:dmda /opt/dmda

ENV TZONE="Europe/London"
RUN apk add --update tzdata \
&& echo "${TZONE}" > /etc/timezone \
&& ln -sf /usr/share/zoneinfo/${TZONE} /etc/localtime

USER dmda
COPY build/libs/dmda.jar /opt/dmda/dmda.jar
WORKDIR /opt/dmda/
HEALTHCHECK CMD curl --fail http://localhost:8080/smtp-status || exit 1
EXPOSE 2601

ENTRYPOINT exec java $JAVA_OPTS -jar dmda.jar