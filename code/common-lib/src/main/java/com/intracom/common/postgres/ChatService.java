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

package com.intracom.common.postgres;

import io.reactiverse.pgclient.PgPool;
import io.reactiverse.reactivex.pgclient.PgRowSet;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * 
 */
public interface ChatService
{
    static ChatService start(PgPool pool,
                             Handler<AsyncResult<ChatService>> handler)
    {
        return new ChatServiceImpl(pool, handler);
    }

    ChatService getMessages(Handler<AsyncResult<PgRowSet>> handler);
}
