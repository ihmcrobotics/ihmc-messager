package us.ihmc.messager.kryo;

import us.ihmc.commons.thread.Notification;
import us.ihmc.log.LogTools;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class KryoAdapterManualServer
{
   public static void main(String[] args)
   {
      AtomicInteger counter = new AtomicInteger();
      KryoAdapter server = KryoAdapter.createServer(KryoAdapterTest.tcpPort);
      Notification serverReceived = new Notification();
      server.setRecievedListener(message -> {
         LogTools.info("Server received: {}", message);
         assertTrue(message instanceof String);
         assertTrue(((String) message).contains("TCP"));

         if (message instanceof String && ((String) message).contains("TCP"))
         {
            serverReceived.set();
         }
      });

      LogTools.info("Connecting...");
      server.connect();

      while (true) Thread.yield();
   }
}
