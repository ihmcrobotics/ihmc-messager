package us.ihmc.messager.kryo;

import com.sun.javafx.geom.Vec3d;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import us.ihmc.commons.thread.Notification;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.log.LogTools;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class KryoAdapterMessageSizeTest
{
   public static final String host = "localhost";
   public static final int tcpPort = 54556;
   public static final int MESSAGE_SIZE = 100;
   final String dataTCP = "Hello TCP ";

   @Test
   public void testLargeMessagesOverKryoAdapter()
   {
      Random random = new Random(19291923999L);
      ArrayList<Pair<String, Vec3d>> listOfStrings = new ArrayList<>();
      for (int i = 0; i < MESSAGE_SIZE; i++)
      {
         listOfStrings.add(new MutablePair<>("num: " + random.nextDouble(), new Vec3d(random.nextDouble(), random.nextDouble(), random.nextDouble())));
      }

      AtomicInteger counter = new AtomicInteger();
      KryoAdapter server = KryoAdapter.createServer(tcpPort);
      Notification serverReceived = new Notification();
      server.setReceivedListener(message -> {
         LogTools.info("Server received: {}", message);
         assertTrue(message instanceof ArrayList);
         assertTrue(((ArrayList) message).size() == MESSAGE_SIZE);

         if (message instanceof ArrayList)
         {
            LogTools.info("Server received list size {}", ((ArrayList) message).size());
            serverReceived.set();
         }
      });

      KryoAdapter client = KryoAdapter.createClient(host, tcpPort);
      Notification clientReceived = new Notification();
      client.setReceivedListener(message -> {
         LogTools.info("Client received: {}", message);
         assertTrue(message instanceof ArrayList);
         assertTrue(((ArrayList) message).size() == MESSAGE_SIZE);

         if (message instanceof ArrayList)
         {
            LogTools.info("Client received list size {}", ((ArrayList) message).size());
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
            sendFromClient(listOfStrings, client);   // sending does not require updating self

         ThreadTools.sleep(5); // give some time to warm up

         while (!serverReceived.poll())
            updateServer(server);          // updating self receives all the messages

         for (int j = 0; j < 5; j++)
            sendFromServer(listOfStrings, server);

         ThreadTools.sleep(5); // give some time to warm up

         while (!clientReceived.poll())
            updateClient(client);
      }

      client.disconnect();
      server.disconnect();
   }

   private void sendFromServer(Object message, KryoAdapter server)
   {
      LogTools.info("Sending from server...");
      server.sendTCP(message);
   }

   private void sendFromClient(Object message, KryoAdapter client)
   {
      LogTools.info("Sending from client...");
      client.sendTCP(message);
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
