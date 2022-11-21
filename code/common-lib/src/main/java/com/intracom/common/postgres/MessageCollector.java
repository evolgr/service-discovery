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
 * Created on: Nov 19, 2022
 *     Author: ekoteva
 */

package com.intracom.common.postgres;

import java.util.function.Function;
import java.util.stream.Collector;

import io.reactiverse.pgclient.Row;
import io.vertx.core.json.JsonArray;

/**
 * 
 */
public final class MessageCollector
{
    public static <T> Collector<Row, ?, JsonArray> jsonArrayCollector(Function<Row, T> rowMapper)
    {
        return Collector.of(JsonArray::new,
                            (jsonArray,
                             row) -> jsonArray.add(rowMapper.apply(row)),
                            (left,
                             right) ->
                            {
                                left.addAll(right);
                                return left;
                            },
                            Collector.Characteristics.IDENTITY_FINISH);
    }
}
