package com.intracom.common;

/**
* Exception to indicate that there was a failure to extract logger with
* specific name
*/
public class UnknownLoggerException extends RuntimeException
{
   private static final long serialVersionUID = 1L;

   public UnknownLoggerException(String message)
   {
       super(message);
   }

   public UnknownLoggerException(String message,
                                 Throwable cause)
   {
       super(message, cause);
   }

   public UnknownLoggerException(Throwable cause)
   {
       super(cause);
   }
}

