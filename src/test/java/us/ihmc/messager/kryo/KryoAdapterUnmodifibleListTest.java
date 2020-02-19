package us.ihmc.messager.kryo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import us.ihmc.commons.thread.Notification;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.log.LogTools;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class KryoAdapterUnmodifibleListTest
{
   public static final String host = "localhost";
   public static final int tcpPort = 54556;
   final List<String> dataTCPTemp = new ArrayList<>();
   {
      dataTCPTemp.add("Hello TCP");
   }
   final List<String> dataTCP = Collections.unmodifiableList(dataTCPTemp);

   @Test
   public void testKryoAdapterUnmodifiableList()
   {
      Assertions.assertTimeoutPreemptively(Duration.ofSeconds(10), () -> runKryoAdapterTest());
   }

   private void runKryoAdapterTest()
   {
      AtomicInteger counter = new AtomicInteger();
      KryoAdapter server = KryoAdapter.createServer(tcpPort);
      Notification serverReceived = new Notification();
      server.setReceivedListener(message -> {
         LogTools.info("Server received: {}", message);
         assertTrue(message instanceof List<?>);
         assertTrue(((List<String>) message).get(0).contains("TCP"));

         if (message instanceof List<?> && ((List<String>) message).get(0).contains("TCP"))
         {
            serverReceived.set();
         }
      });

      KryoAdapter client = KryoAdapter.createClient(host, tcpPort);
      Notification clientReceived = new Notification();
      client.setReceivedListener(message -> {
         LogTools.info("Client received: {}", message);
         assertTrue(message instanceof List<?>);
         assertTrue(((List<String>) message).get(0).contains("TCP"));

         if (message instanceof List<?> && ((List<String>) message).get(0).contains("TCP"))
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
      server.sendTCP(dataTCP);
   }

   private void sendFromClient(AtomicInteger counter, KryoAdapter client)
   {
      LogTools.info("Sending from client...");
      client.sendTCP(dataTCP);
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
