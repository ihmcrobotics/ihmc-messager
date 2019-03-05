package us.ihmc.messager.kryo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import us.ihmc.commons.thread.Notification;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.log.LogTools;

import java.util.concurrent.atomic.AtomicInteger;

public class KryoAdapterTest
{
   public static final String host = "localhost";
   public static final int tcpPort = 54556;
   final String dataTCP = "Hello TCP ";

   @Test
   public void testKryoAdapter()
   {
      AtomicInteger counter = new AtomicInteger();
      KryoAdapter server = KryoAdapter.createServer(tcpPort);
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

      KryoAdapter client = KryoAdapter.createClient(host, tcpPort);
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
      server.connect();

      while (!client.isConnected() || !server.isConnected());
      LogTools.info("Connected!");

      for (int i = 0; i < 10; i++)
      {
         for (int j = 0; j < 5; j++)
            sendFromClient(counter, client);   // sending does not require updating self

         ThreadTools.sleep(5); // give some time to warm up

         while (!serverReceived.poll())
            updateServer(server);          // updating self receives all the messages

         for (int j = 0; j < 5; j++)
            sendFromServer(counter, server);

         ThreadTools.sleep(5); // give some time to warm up

         while (!clientReceived.poll())
            updateClient(client);
      }

      client.disconnect();
      server.disconnect();
   }

   private void sendFromServer(AtomicInteger counter, KryoAdapter server)
   {
      LogTools.info("Sending from server...");
      server.sendTCP(dataTCP + counter.incrementAndGet());
   }

   private void sendFromClient(AtomicInteger counter, KryoAdapter client)
   {
      LogTools.info("Sending from client...");
      client.sendTCP(dataTCP + counter.incrementAndGet());
   }

   private void updateServer(KryoAdapter server)
   {
      LogTools.info("Updating server...");
      server.update();
   }

   private void updateClient(KryoAdapter client)
   {
      LogTools.info("Updating client...");
      client.update();
   }
}
