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
 * Created on: Nov 14, 2022
 *     Author: zpalele
 */

package com.intracom.common;

import io.reactivex.functions.Consumer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;

public class WebClientCBuilder
{
    final WebClientOptions options = new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
    String hostname;

    public WebClientCBuilder()
    {
        // empty constructor
    }

    public WebClientCBuilder withOptions(Consumer<WebClientOptions> options)
    {
        try
        {
            options.accept(this.options);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Failed to set WebClient otpions", e);
        }
        return this;
    }

    public WebClientC build(Vertx vertx)
    {
        return new WebClientC(vertx, this);
    }
}
