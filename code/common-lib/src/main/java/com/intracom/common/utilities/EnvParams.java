package com.intracom.common.utilities;

import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnvParams
{
   private static final Logger log = LoggerFactory.getLogger(EnvParams.class);

   private static Predicate<String> validator = value -> value.length() < 512;

   public static String get(final String name)
   {
       return get(name, null, validator);
   }

   public static String get(final String name,
                            final String defaultValue)
   {
       return get(name, defaultValue, validator);
   }

   public static <T> String get(final String name,
                                final T defaultValue)
   {
       return get(name, defaultValue, validator);
   }

   public static String get(final String name,
                            final Predicate<String> validator)
   {
       return get(name, null, validator);
   }

   public static <T> String get(final String name,
                                final T defaultValue,
                                final Predicate<String> validator)
   {
       final String value = System.getenv(name);

       if (value == null)
           return defaultValue == null ? null : defaultValue.toString();

       if (!validator.test(value))
       {
           log.error("Value of environment variable '{}' is invalid: '{}', returning default value '{}'.", name, value, defaultValue);
           return defaultValue == null ? null : defaultValue.toString();
       }

       return value;
   }

   private EnvParams()
   {
   }
}