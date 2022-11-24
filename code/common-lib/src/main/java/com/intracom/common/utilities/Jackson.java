package com.intracom.common.utilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

public class Jackson
{
    private static final JsonMapper OM = alteredObjectMapper();

    private Jackson()
    {
    }

    public static JsonMapper alteredObjectMapper()
    {
        return JsonMapper.builder()
                         .addModules(new ParameterNamesModule(), //
                                     new Jdk8Module(), //
                                     new JavaTimeModule(), //
                                     new JodaModule()) //
                         .serializationInclusion(JsonInclude.Include.NON_NULL)
                         .configure(DeserializationFeature.USE_LONG_FOR_INTS, true)
                         .build();
    }

    public static JsonMapper om()
    {
        return OM;
    }
}