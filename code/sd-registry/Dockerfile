FROM armdockerhub.rnd.ericsson.se/openjdk
RUN mkdir -p /java/exec
ADD sd-registry.jar /java/exec/sd-registry.jar
CMD ["java","-jar" ,"/java/exec/sd-registry.jar"]
