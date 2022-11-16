package com.intracom.common;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intracom.common.ConfigmapWatch.ConfigmapFile;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DynamicConfigmap
{
    private static final Logger log = LoggerFactory.getLogger(DynamicConfigmap.class);
    private final String loggerName;
    private final String containerName;
    private final ConfigmapWatch configmap;
    private final Disposable cd;

    private static final ObjectMapper json = Jackson.om();

    /**
     * Log level changer for changing log level according to changes identified by
     * configmap
     * 
     * @param configmap     the configmap watch operation
     * @param loggerName    the name of logger to be updated
     * @param containerName the container name that exists in log control configmap
     */
    public DynamicConfigmap(ConfigmapWatch configmap,
                           String loggerName,
                           String containerName)
    {
        this.configmap = configmap;
        this.loggerName = loggerName;
        this.containerName = containerName;

        this.cd = this.logControl().retryWhen(errors -> errors.flatMap(e ->
        {
            log.warn("Could not watch logControl configmap file, retrying..", e);
            return Flowable.timer(10, TimeUnit.SECONDS);
        }))
                      .repeat()
                      .doOnSubscribe(sub -> log.info("Starting dynamic monitoring of log control changes"))
                      .doOnComplete(() -> log.info("Stoping dynamic monitoring of log control changes"))
                      .subscribe();
    }

    /**
     * Log level changer for changing log level according to changes identified by
     * configmap with default ROOT logger
     * 
     * @param configmap     the configmap watch operation
     * @param containerName the container name that exists in log control configmap
     */
    public DynamicConfigmap(ConfigmapWatch configmap,
                           String containerName)
    {
        this(configmap, "ROOT", containerName);
    }

    /**
     * Stop monitoring logcontrol severities
     * 
     */
    private void terminate()
    {
        Completable.fromAction(this.cd::dispose).cache();
    }

    public void close()
    {
        this.terminate();
    }

    /**
     * Watch log control configmap changes and apply log level change to specific
     * container according to the severity defined
     * 
     * @return flowable of current log levels as they are recognized by logback
     */
    public Flowable<Level> logControl()
    {
        return this.configmap.watch() //
                             .map(ConfigmapFile::getData)
                             .doOnNext(next -> log.debug("New configmap change identified"))
                             .doOnSubscribe(sub -> log.debug("Starting monitoring configmap file changes"))
                             .doOnTerminate(() -> log.debug("Terminating monitoring configmap file changes"))
                             .doOnComplete(() -> log.debug("Stoping monitoring configmap file changes"))
                             .doOnError(e -> log.error("Error occured while monitoring configmap file changes", e))
                             .onBackpressureBuffer()
                             .map(this::updateSeverity)
                             .subscribeOn(Schedulers.io());
    }

    /**
     * Use the input data from the log control and change the specific logger
     * severity according to input data
     * 
     * @param data log control latest data from configmap file
     * @return current log level recognized by logback
     */
    private Level updateSeverity(String data)
    {
        var lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        var logger = lc.exists(this.loggerName);
        if (logger == null)
            throw new UnknownLoggerException("Failed to identify logger " + this.loggerName);
        log.debug("Logger {} identified.", this.loggerName);

        var ls = LogSeverity.builder() // create new log severity builder
                            .withContainer(this.containerName) // set the container name
                            .withDefaultSeverity() // use default info severity
                            .build();
        try
        {
            log.debug("Log control data to check: {}", data);
            json.configure(DeserializationFeature.USE_JAVA_ARRAY_FOR_JSON_ARRAY, true);

            var logSeverities = Arrays.stream(json.readValue(data, LogSeverity[].class)) // stream service containers log severity
                                      .filter(s -> s.getContainer().equals(this.containerName)) // select current container
                                      .map(s ->
                                      {
                                          // if configmap does not contain info/debug/error use default info
                                          if (!s.getSeverity().equalsIgnoreCase(Level.INFO.levelStr) //
                                              && !s.getSeverity().equalsIgnoreCase(Level.DEBUG.levelStr) //
                                              && !s.getSeverity().equalsIgnoreCase(Level.ERROR.levelStr))
                                          {
                                              log.error("Invalid log level {} requested for container {}, default severity INFO will be used.",
                                                        s.getSeverity(),
                                                        this.containerName);
                                              s = LogSeverity.builder(s) // create new builder
                                                             .withDefaultSeverity() // enforce default severity
                                                             .build(); // build new log severity
                                          }
                                          return s;
                                      })
                                      .collect(Collectors.toList());

            // check if container name is missing from logControl
            if (logSeverities.isEmpty())
                log.error("Failed to identify {} container, default severity INFO will be used", this.containerName);
            // check if multiple containers exist in logControl with the same name
            else if (logSeverities.size() > 1)
                log.error("Multiple containers {} identified, default severity INFO will be used", this.containerName);
            else
                ls = logSeverities.get(0);
        }
        catch (JsonProcessingException e)
        {
            // in case of invalid configmap format, use default info
            log.error("Failed to process log control data, default severity INFO will be used", e);
        }

        logger.setLevel(Level.WARN);
        log.warn("Changing logger {} of {} container log level to {}", this.loggerName, this.containerName, ls.getLevel());
        logger.setLevel(ls.getLevel()); // if container does not exist in configmap default info is used
        return logger.getLevel();
    }
}
