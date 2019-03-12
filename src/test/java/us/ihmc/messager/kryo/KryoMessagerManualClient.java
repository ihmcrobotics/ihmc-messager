package us.ihmc.messager.kryo;

import org.apache.commons.lang3.mutable.MutableObject;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.examples.EnglishPerson;
import us.ihmc.messager.examples.FrenchPerson;

public class KryoMessagerManualClient
{
   public static void main(String[] args)
   {
      int tcpPort = 54557;
      MessagerAPIFactory api = new MessagerAPIFactory();
      api.createRootCategory("TranslatorExample");
      api.includeMessagerAPIs(EnglishPerson.EnglishAPI, FrenchPerson.FrenchAPI);
      Messager clientMessager = KryoMessager.createClient(api.getAPIAndCloseFactory(), "localhost", tcpPort, "ManualClient", 5);
      ExceptionTools.handle(() -> clientMessager.startMessager(), DefaultExceptionHandler.RUNTIME_EXCEPTION);

      LogTools.info("Client connecting...");

      while (!clientMessager.isMessagerOpen());  // wait for connection

      LogTools.info("Connected!");

      while (true) Thread.yield();
   }
}
