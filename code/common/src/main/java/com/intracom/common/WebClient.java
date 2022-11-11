package com.intracom.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

public class WebClient
{
    private static final Logger log = LoggerFactory.getLogger(WebClient.class);

    public WebClient()
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
