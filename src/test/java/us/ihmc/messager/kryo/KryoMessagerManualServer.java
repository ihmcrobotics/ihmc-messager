package us.ihmc.messager.kryo;

import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.examples.EnglishPerson;
import us.ihmc.messager.examples.FrenchPerson;

public class KryoMessagerManualServer
{
   public KryoMessagerManualServer()
   {
      int tcpPort = 54557;
      MessagerAPIFactory api = new MessagerAPIFactory();
      api.createRootCategory("TranslatorExample");
      api.includeMessagerAPIs(EnglishPerson.EnglishAPI, FrenchPerson.FrenchAPI);
      Messager serverMessager = KryoMessager.createServer(api.getAPIAndCloseFactory(), tcpPort, "ManualServer", 5);
      ExceptionTools.handle(() -> serverMessager.startMessager(), DefaultExceptionHandler.RUNTIME_EXCEPTION);

      LogTools.info("Server connecting...");


      while (!serverMessager.isMessagerOpen());  // wait for connection

      LogTools.info("Connected!");

      while (true) Thread.yield();
   }

   public static void main(String[] args)
   {
      new KryoMessagerManualServer();
   }
}
