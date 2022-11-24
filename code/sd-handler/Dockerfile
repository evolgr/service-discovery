FROM armdockerhub.rnd.ericsson.se/openjdk
RUN mkdir -p /java/exec
ADD sd-handler.jar /java/exec/sd-handler.jar
CMD ["java","-jar" ,"/java/exec/sd-handler.jar"]
