package us.ihmc.messager.kryo;

import com.esotericsoftware.kryonet.*;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.log.LogTools;

import java.io.IOException;

public class SimplestKryoManualTest
{
   static public String host = "localhost";
   static public int tcpPort = 54555, udpPort = 54777;
   final String dataTCP = "Hello TCP";
   final String dataUDP = "Hello UDP";

   public void test() throws IOException
   {
      Server server = new Server(16384, 8192);
      server.getKryo().register(String.class);
      new Thread(server, "Server").start();
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
      client.getKryo().register(String.class);
      new Thread(client, "Client").start();
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

      client.connect(5000, host, tcpPort, udpPort);

      ThreadTools.sleep(5);

      client.stop();
      server.stop();
   }

   public static void main(String[] args) throws IOException
   {
      new SimplestKryoManualTest().test();
   }
}
