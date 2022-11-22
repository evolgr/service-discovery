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

package com.intracom.registry;

import java.util.Set;

import com.intracom.common.web.WebServer;
import com.intracom.model.Service;

import io.reactivex.Completable;

/**
 * 
 */
public class Registrations
{

    private Set<Service> services;

    public Registrations(WebServer server)
    {
        // create webserver
    }

    public Set<Service> getService(String function)
    {
        // return services that belong to specific function
        return services;
    }

    public Completable start()
    {
        // start webserver and update services
        return null;
    }

    public Completable stop()
    {
        // stop webserver
        return null;
    }
}
