/**
 * COPYRIGHT ERICSSON GMBH 2022
 *
 * The copyright to the computer program(s) herein is the property
 * of Ericsson GmbH, Germany.
 *
 * The program(s) may be used and/or copied only with the written
 * permission of Ericsson GmbH in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * Created on: Nov 21, 2022
 *     Author: ekoteva
 */

package com.intracom.common.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class EnvironmentParameters
{
    private static final Logger log = LoggerFactory.getLogger(EnvironmentParameters.class);

    public static String get(final String parameterName)
    {
        return get(parameterName, null);
    }

    public static String get(final String parameterName,
                             final String defaultParameterValue)
    {
        return get(parameterName, defaultParameterValue);
    }

    public static <T> String get(final String parameterName,
                                 final T defaultParameterValue)
    {
        final String parameterValue = System.getenv(parameterName);
        if (parameterValue != null)
            return parameterValue;

        log.error("Failed to extract value of environment parameter {}", parameterName);

        if (defaultParameterValue != null)
        {
            log.info("Returning default value {} for parameter {}", defaultParameterValue, parameterName);
            return defaultParameterValue.toString();
        }

        return null;
    }
}
