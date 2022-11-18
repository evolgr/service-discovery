package com.intracom.handler;

public class MonitorContext
{
    public static final String HANDLER_SERVICE = "handler";
    public static final int HANDLER_PORT_HTTP_INTERNAL = 8080;

    public enum Operation
    {
        MODEL("/model");

        private final String op;

        public String getName()
        {
            return this.op;
        }

        private Operation(final String name)
        {
            this.op = name;
        }
    }
}
