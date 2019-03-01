package us.ihmc.messager.kryo;

import us.ihmc.commons.RunnableThatThrows;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionHandler;

class ExceptionTools
{
   /**
    * One-liner exception handling when used with {@link DefaultExceptionHandler}.
    *
    * @param runnable
    * @param exceptionHandler
    */
   public static void handle(RunnableThatThrows runnable, ExceptionHandler exceptionHandler)
   {
      try
      {
         runnable.run();
      }
      catch (Throwable e)
      {
         exceptionHandler.handleException(e);
      }
   }
}
