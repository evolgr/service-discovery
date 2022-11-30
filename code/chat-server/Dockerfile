FROM armdockerhub.rnd.ericsson.se/openjdk

RUN mkdir -p /chat-server &&\
    chmod -R g=u /chat-server

COPY target/libs /chat-server/libs
COPY target/classes /chat-server/classes

WORKDIR /chat-server

ENTRYPOINT ["/bin/bash", "-c", "java -cp /chat-server/libs/*:/chat-server/classes -server -XX:+ErrorFileToStderr -XX:+UseG1GC -XX:MaxRAMPercentage=80 -Dlogback.configurationFile=/chat-server/classes/logback.xml com.intracom.chat.Server"]

#CMD tail -f /dev/null