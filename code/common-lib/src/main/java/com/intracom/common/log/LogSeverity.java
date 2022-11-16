package com.intracom.common.log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import ch.qos.logback.classic.Level;
import io.vertx.core.json.JsonObject;


public class LogSeverity
{
    @JsonProperty("container")
    private String container;

    @JsonProperty("severity")
    private String severity;

    private static final String CONTAINER_KEY = "container";
    private static final String SEVERITY_KEY = "severity";
    private static final String LOG_LEVEL_KEY = "level";
    private static final String DEFAULT_SEVERITY = "info";

    public LogSeverity()
    {
        // empty constructor
    }

    /**
     * 
     * @param container
     * @param severity
     */
    public LogSeverity(final LogSeverity logSeverity)
    {
        super();
        this.container = logSeverity.getContainer();
        this.severity = logSeverity.getSeverity();
    }

    /**
     * 
     * @param container
     * @param severity
     */
    public LogSeverity(final String container,
                       final String severity)
    {
        super();
        this.container = container;
        this.severity = severity;
    }

    /**
     * 
     * @param container
     */
    public LogSeverity(final String container)
    {
        super();
        this.container = container;
        this.severity = DEFAULT_SEVERITY;
    }

    /**
     * An identifier uniquely identifying the container name of each service.
     * (Required)
     * 
     */
    @JsonProperty("container")
    public String getContainer()
    {
        return this.container;
    }

    @JsonProperty("severity")
    public String getSeverity()
    {
        return this.severity;
    }

    @JsonIgnore
    public Level getLevel()
    {
        return Level.valueOf(this.severity);
    }

    @JsonIgnore
    public static LogSeverityBuilder builder()
    {
        return new LogSeverityBuilder();
    }

    @JsonIgnore
    public static LogSeverityBuilder builder(LogSeverity source)
    {
        return new LogSeverityBuilder(source);
    }

    @Override
    public String toString()
    {
        var ls = new JsonObject();
        ls.put(CONTAINER_KEY, this.container);
        ls.put(SEVERITY_KEY, this.severity);
        ls.put(LOG_LEVEL_KEY, this.getLevel());
        return ls.encodePrettily();
    }

    public static class LogSeverityBuilder
    {
        private String container;
        private String severity;

        public LogSeverityBuilder()
        {
            // empty constructor
        }

        public LogSeverityBuilder(LogSeverity source)
        {
            this.container = source.container;
            this.severity = source.severity;
        }

        public LogSeverityBuilder withContainer(String container)
        {
            this.container = container;
            return this;
        }

        public LogSeverityBuilder withSeverity(String severity)
        {
            this.severity = severity;
            return this;
        }

        public LogSeverityBuilder withDefaultSeverity()
        {
            this.severity = Level.INFO.levelStr;
            return this;
        }

        public LogSeverity build()
        {
            return new LogSeverity(this.container, this.severity);
        }
    }
}
