FROM armdockerhub.rnd.ericsson.se/openjdk
RUN mkdir -p /java/exec
ADD chat-server.jar /java/exec/chat-server.jar
CMD ["java","-jar" ,"/java/exec/chat-server.jar"]
