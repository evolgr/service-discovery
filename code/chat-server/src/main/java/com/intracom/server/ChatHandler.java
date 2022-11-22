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

import com.intracom.common.web.WebServer;

import io.reactivex.Completable;

/**
 * 
 */
public class ChatHandler
{

    public ChatHandler(WebServer server)
    {
        // create web server
    }

    public Completable start()
    {
        // respond with dummy message
        return null;
    }

    public Completable stop()
    {
        // close webserver
        return null;
    }
}
