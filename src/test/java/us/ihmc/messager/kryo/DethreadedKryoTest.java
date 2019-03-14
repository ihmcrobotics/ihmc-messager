package us.ihmc.messager.kryo;

import com.esotericsoftware.kryonet.*;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.jupiter.api.Test;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.log.LogTools;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DethreadedKryoTest
{
   static public String host = "localhost";
   static public int tcpPort = 54555, udpPort = 54777;
   final String dataTCP = "Hello TCP";
   final String dataUDP = "Hello UDP";

   @Test
   public void testKryoDethreaded() throws IOException
   {
      Server server = new Server(16384, 8192);
      server.getKryo().setRegistrationRequired(false);
      server.bind(tcpPort, udpPort);
      server.addListener(new Listener()
      {
         public void connected(Connection connection)
         {
            connection.sendTCP(dataTCP);
            connection.sendUDP(dataUDP); // Note UDP ping pong stops if a UDP packet is lost.
         }

         public void received(Connection connection, Object object)
         {
            LogTools.info("Server received: {}", object);
            if (object instanceof String && ((String) object).contains("TCP"))
            {
               connection.sendTCP(dataTCP);
            }
            else if (object instanceof String && ((String) object).contains("UDP"))
            {
               connection.sendUDP(dataUDP);
            }
         }
      });

      Client client = new Client(16384, 8192);
      client.getKryo().setRegistrationRequired(false);
      client.addListener(new Listener()
      {
         public void received(Connection connection, Object object)
         {
            LogTools.info("Client received: {}", object);
            if (object instanceof String && ((String) object).contains("TCP"))
            {
               connection.sendTCP(dataTCP);
            }
            else if (object instanceof String && ((String) object).contains("UDP"))
            {
               connection.sendUDP(dataUDP);
            }
         }
      });

      AtomicBoolean connected = new AtomicBoolean();
      new Thread(() -> ExceptionTools.handle(() ->
                                             {
                                                client.connect(5000, host, tcpPort, udpPort);
                                                connected.set(true);
                                             }, DefaultExceptionHandler.RUNTIME_EXCEPTION)).start();
      new Thread(() -> ExceptionTools.handle(() ->
                                             {
                                                while (!connected.get())
                                                {
                                                   updateClient(client);
                                                }
                                             }, DefaultExceptionHandler.RUNTIME_EXCEPTION)).start();

      new Thread(() -> ExceptionTools.handle(() ->
                                             {
                                                while (!connected.get())
                                                {
                                                   updateServer(server);
                                                }
                                             }, DefaultExceptionHandler.RUNTIME_EXCEPTION)).start();
      LogTools.info("Connecting...");
      while (!connected.get());
      LogTools.info("Connected!");

      for (int i = 0; i < 10; i++)  // hopefully there is a guarantee that after 4 calls all data has been transferred.
      {
         updateServer(server);
         updateClient(client);
      }

      client.stop();
      server.stop();
   }

   private void updateServer(Server server) throws IOException
   {
      LogTools.info("Updating server...");
      server.update(250);
   }

   private void updateClient(Client client) throws IOException
   {
      LogTools.info("Updating client...");
      client.update(250);
   }
}
