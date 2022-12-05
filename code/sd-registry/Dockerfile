FROM armdockerhub.rnd.ericsson.se/openjdk

RUN mkdir -p /sd-registry &&\
    chmod -R g=u /sd-registry
    
COPY target/libs /sd-registry/libs
COPY target/classes /sd-registry/classes

WORKDIR /sd-registry

ENTRYPOINT ["/bin/bash", "-c", "java -cp /sd-registry/libs/*:/sd-registry/classes -server -XX:+ErrorFileToStderr -XX:+UseG1GC -XX:MaxRAMPercentage=80 -Dlogback.configurationFile=/sd-registry/classes/logback.xml com.intracom.sd.Registry"]

#CMD tail -f /dev/null