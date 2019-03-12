package us.ihmc.messager.kryo;

import us.ihmc.commons.thread.Notification;
import us.ihmc.log.LogTools;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class KryoAdapterManualClient
{
   public static void main(String[] args)
   {
      KryoAdapter client = KryoAdapter.createClient(KryoAdapterTest.host, KryoAdapterTest.tcpPort);
      Notification clientReceived = new Notification();
      client.setRecievedListener(message -> {
         LogTools.info("Client received: {}", message);
         assertTrue(message instanceof String);
         assertTrue(((String) message).contains("TCP"));

         if (message instanceof String && ((String) message).contains("TCP"))
         {
            clientReceived.set();
         }
      });

      LogTools.info("Connecting...");
      client.connect();

      while (true) Thread.yield();
   }
}
