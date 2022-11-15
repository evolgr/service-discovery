
package com.intracom.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.processing.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.joda.time.DateTime;


/**
 * Chat Message
 * <p>
 * Definition of a Chat message.
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "name",
    "message",
    "timestamp",
    "recipients"
})
@Generated("jsonschema2pojo")
public class MyApi {

    /**
     * The user name.
     * (Required)
     * 
     */
    @JsonProperty("name")
    @JsonPropertyDescription("The user name.")
    private String name;
    /**
     * Extra information providing further insight about the fault.
     * (Required)
     * 
     */
    @JsonProperty("message")
    @JsonPropertyDescription("Extra information providing further insight about the fault.")
    private String message;
    /**
     * The timestamp of when the fault indication was created, according to ISO8601 format: YYYY-MM-DDTHH:MM:SS.mmmmmmz (z is the relative time zone offset in hours and minutes to UTC in the format +hh:mm or -hh:mm. If UTC is used z will be 'Z' instead of '+00:00').
     * (Required)
     * 
     */
    @JsonProperty("timestamp")
    @JsonPropertyDescription("The timestamp of when the fault indication was created, according to ISO8601 format: YYYY-MM-DDTHH:MM:SS.mmmmmmz (z is the relative time zone offset in hours and minutes to UTC in the format +hh:mm or -hh:mm. If UTC is used z will be 'Z' instead of '+00:00').")
    private DateTime timestamp;
    /**
     * Message recipients
     * 
     */
    @JsonProperty("recipients")
    @JsonPropertyDescription("Message recipients")
    private Recipients recipients;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    /**
     * No args constructor for use in serialization
     * 
     */
    public MyApi() {
    }

    /**
     * 
     * @param source
     */
    public MyApi(MyApi source) {
        super();
        this.name = source.name;
        this.message = source.message;
        this.timestamp = source.timestamp;
        this.recipients = source.recipients;
    }

    /**
     * 
     * @param recipients
     * @param name
     * @param message
     * @param timestamp
     */
    public MyApi(String name, String message, DateTime timestamp, Recipients recipients) {
        super();
        this.name = name;
        this.message = message;
        this.timestamp = timestamp;
        this.recipients = recipients;
    }

    /**
     * The user name.
     * (Required)
     * 
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     * Extra information providing further insight about the fault.
     * (Required)
     * 
     */
    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    /**
     * The timestamp of when the fault indication was created, according to ISO8601 format: YYYY-MM-DDTHH:MM:SS.mmmmmmz (z is the relative time zone offset in hours and minutes to UTC in the format +hh:mm or -hh:mm. If UTC is used z will be 'Z' instead of '+00:00').
     * (Required)
     * 
     */
    @JsonProperty("timestamp")
    public DateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Message recipients
     * 
     */
    @JsonProperty("recipients")
    public Optional<Recipients> getRecipients() {
        return Optional.ofNullable(recipients);
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(MyApi.class.getName()).append('@').append(Integer.toHexString(System.identityHashCode(this))).append('[');
        sb.append("name");
        sb.append('=');
        sb.append(((this.name == null)?"<null>":this.name));
        sb.append(',');
        sb.append("message");
        sb.append('=');
        sb.append(((this.message == null)?"<null>":this.message));
        sb.append(',');
        sb.append("timestamp");
        sb.append('=');
        sb.append(((this.timestamp == null)?"<null>":this.timestamp));
        sb.append(',');
        sb.append("recipients");
        sb.append('=');
        sb.append(((this.recipients == null)?"<null>":this.recipients));
        sb.append(',');
        sb.append("additionalProperties");
        sb.append('=');
        sb.append(((this.additionalProperties == null)?"<null>":this.additionalProperties));
        sb.append(',');
        if (sb.charAt((sb.length()- 1)) == ',') {
            sb.setCharAt((sb.length()- 1), ']');
        } else {
            sb.append(']');
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = ((result* 31)+((this.name == null)? 0 :this.name.hashCode()));
        result = ((result* 31)+((this.additionalProperties == null)? 0 :this.additionalProperties.hashCode()));
        result = ((result* 31)+((this.message == null)? 0 :this.message.hashCode()));
        result = ((result* 31)+((this.recipients == null)? 0 :this.recipients.hashCode()));
        result = ((result* 31)+((this.timestamp == null)? 0 :this.timestamp.hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if ((other instanceof MyApi) == false) {
            return false;
        }
        MyApi rhs = ((MyApi) other);
        return ((((((this.name == rhs.name)||((this.name!= null)&&this.name.equals(rhs.name)))&&((this.additionalProperties == rhs.additionalProperties)||((this.additionalProperties!= null)&&this.additionalProperties.equals(rhs.additionalProperties))))&&((this.message == rhs.message)||((this.message!= null)&&this.message.equals(rhs.message))))&&((this.recipients == rhs.recipients)||((this.recipients!= null)&&this.recipients.equals(rhs.recipients))))&&((this.timestamp == rhs.timestamp)||((this.timestamp!= null)&&this.timestamp.equals(rhs.timestamp))));
    }

    public static class MyApiBuilder
        extends MyApi.MyApiBuilderBase<MyApi>
    {


        public MyApiBuilder() {
            super();
        }

        public MyApiBuilder(MyApi source) {
            super(source);
        }

        public MyApiBuilder(String name, String message, DateTime timestamp, Recipients recipients) {
            super(name, message, timestamp, recipients);
        }

    }

    public static abstract class MyApiBuilderBase<T extends MyApi >{

        protected T instance;

        @SuppressWarnings("unchecked")
        public MyApiBuilderBase() {
            // Skip initialization when called from subclass
            if (this.getClass().equals(MyApi.MyApiBuilder.class)) {
                this.instance = ((T) new MyApi());
            }
        }

        @SuppressWarnings("unchecked")
        public MyApiBuilderBase(MyApi source) {
            // Skip initialization when called from subclass
            if (this.getClass().equals(MyApi.MyApiBuilder.class)) {
                this.instance = ((T) new MyApi(source));
            }
        }

        @SuppressWarnings("unchecked")
        public MyApiBuilderBase(String name, String message, DateTime timestamp, Recipients recipients) {
            // Skip initialization when called from subclass
            if (this.getClass().equals(MyApi.MyApiBuilder.class)) {
                this.instance = ((T) new MyApi(name, message, timestamp, recipients));
            }
        }

        public T build() {
            T result;
            result = this.instance;
            this.instance = null;
            return result;
        }

        public MyApi.MyApiBuilderBase withName(String name) {
            ((MyApi) this.instance).name = name;
            return this;
        }

        public MyApi.MyApiBuilderBase withMessage(String message) {
            ((MyApi) this.instance).message = message;
            return this;
        }

        public MyApi.MyApiBuilderBase withTimestamp(DateTime timestamp) {
            ((MyApi) this.instance).timestamp = timestamp;
            return this;
        }

        public MyApi.MyApiBuilderBase withRecipients(Recipients recipients) {
            ((MyApi) this.instance).recipients = recipients;
            return this;
        }

        public MyApi.MyApiBuilderBase withAdditionalProperty(String name, Object value) {
            ((MyApi) this.instance).additionalProperties.put(name, value);
            return this;
        }

    }

}
