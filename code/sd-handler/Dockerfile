FROM armdockerhub.rnd.ericsson.se/openjdk

RUN mkdir -p /sd-handler &&\
    chmod -R g=u /sd-handler
    
COPY target/libs /sd-handler/libs
COPY target/classes /sd-handler/classes

WORKDIR /sd-handler

ENTRYPOINT ["/bin/bash", "-c", "java -cp /sd-handler/libs/*:/sd-handler/classes -server -XX:+ErrorFileToStderr -XX:+UseG1GC -XX:MaxRAMPercentage=80 -Dlogback.configurationFile=/sd-handler/classes/logback.xml com.intracom.sd.Handler"]

#CMD tail -f /dev/null