package us.ihmc.messager.kryo;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import us.ihmc.commons.RunnableThatThrows;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Unifies the API of Kryonet Server and Client.
 */
public class KryoAdapter
{
   private Listener kryoListener = new KryoListener();
   private Consumer receivedConsumer;
   private final ArrayList<Consumer> connectionStateListeners = new ArrayList<>();

   private final BooleanSupplier isConnectedSupplier;
   private final RunnableThatThrows updater;
   private final RunnableThatThrows connector;
   private final RunnableThatThrows disconnector;
   private final Consumer tcpSender;

   public static KryoAdapter createServer(int tcpPort)
   {
      return new KryoAdapter(tcpPort);
   }

   public static KryoAdapter createClient(String serverAddress, int tcpPort)
   {
      return new KryoAdapter(serverAddress, tcpPort);
   }

   private KryoAdapter(int tcpPort)
   {
      Server server = new Server();
      server.addListener(kryoListener);
      server.getKryo().setRegistrationRequired(false);
      isConnectedSupplier = () -> server.getConnections().length > 0;
      updater = () -> server.update(250);
      connector = () -> server.bind(tcpPort);
      disconnector = () -> server.close();
      tcpSender = message -> server.sendToAllTCP(message);
   }

   private KryoAdapter(String serverAddress, int tcpPort)
   {
      Client client = new Client();
      client.addListener(kryoListener);
      client.getKryo().setRegistrationRequired(false);
      isConnectedSupplier = () -> client.isConnected();
      updater = () -> client.update(250);
      connector = () -> client.connect(5000, serverAddress, tcpPort);
      disconnector = () -> client.close();
      tcpSender = message -> client.sendTCP(message);
   }

   class KryoListener extends Listener
   {
      @Override
      public void received(Connection connection, Object object)
      {
         receivedConsumer.accept(object);
      }

      @Override
      public void connected(Connection connection)
      {
         connectionStateListeners.forEach(connectionStateListener -> connectionStateListener.accept(true));
      }

      @Override
      public void disconnected(Connection connection)
      {
         connectionStateListeners.forEach(connectionStateListener -> connectionStateListener.accept(false));
      }
   }

   public void connect()
   {
      new Thread(() -> startNonBlockingConnect()).start();  // this is the "kickstart" method required to
      new Thread(() -> waitForConnection()).start();        // get Kryo to connect
   }

   private void startNonBlockingConnect()
   {
      ExceptionTools.handle(() -> connector.run(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
   }

   private void waitForConnection()
   {
      while (!isConnectedSupplier.getAsBoolean())
      {
         ExceptionTools.handle(() -> updater.run(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
      }
   }

   public void disconnect()
   {
      ExceptionTools.handle(() -> disconnector.run(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
   }

   public void update()
   {
      ExceptionTools.handle(() -> updater.run(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
   }

   public void sendTCP(Object object)
   {
      tcpSender.accept(object);
   }

   public boolean isConnected()
   {
      return isConnectedSupplier.getAsBoolean();
   }

   public void setRecievedListener(Consumer receivedConsumer)
   {
      this.receivedConsumer = receivedConsumer;
   }

   public void addConnectionStateListener(Consumer<Boolean> connectionStateListener)
   {
      connectionStateListeners.add(connectionStateListener);
   }
}
