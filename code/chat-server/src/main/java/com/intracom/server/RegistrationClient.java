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
 * Created on: Nov 22, 2022
 *     Author: ekoteva
 */

package com.intracom.server;

import com.intracom.common.web.WebClientC;

import io.reactivex.Completable;

/**
 * 
 */
public class RegistrationClient
{

    public RegistrationClient(WebClientC client,
                              String function,
                              String podName,
                              String host,
                              String port)
    {
        // create webclient
    }

    public Completable start()
    {
        // continously send registrations every 1 min
        // to sd-registry service
        // with data according to ServiceRegistry.java
        // autogenerate timestamp
        return null;
    }

    public Completable stop()
    {
        // close web client
        return null;
    }
}
