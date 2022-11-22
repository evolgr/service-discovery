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

import java.net.URI;

import com.intracom.common.web.WebServer;

import io.reactivex.Completable;

/**
 * 
 */
public class RequestHandler
{
    private static final URI REGISTRY_URI = URI.create("/registrations");

    public RequestHandler(WebServer server,
                          Registrations registrations)
    {
        // create webserver
    }

    public Completable start()
    {
        // start webserver and listen on path /registrations
        // return registrations set of services
        return null;
    }

    public Completable stop()
    {
        // stop webserver
        return null;
    }
}
