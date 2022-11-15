package com.intracom.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

public class Handler
{
    private static final Logger log = LoggerFactory.getLogger(Handler.class);

    public Handler()
    {
		// empty constructor
	}

	public static void main(String args[]) throws InterruptedException {
		while(true)
		{
			log.info("this is a simple example");
			TimeUnit.SECONDS.sleep(10);
		}
	}
}
