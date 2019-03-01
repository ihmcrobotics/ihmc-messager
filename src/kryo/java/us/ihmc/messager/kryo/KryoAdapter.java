package us.ihmc.messager.kryo;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import us.ihmc.commons.RunnableThatThrows;
import us.ihmc.commons.exception.DefaultExceptionHandler;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Pretty much all that ihmc-communication does :rolling_on_floor_laughing:
 */
public class KryoAdapter
{
   private Listener kryoListener = new KryoListener();
   private final BooleanSupplier isConnectedSupplier;
   private final Consumer tcpSender;
   private final Runnable connector;
   private final Runnable disconnector;
   private final Runnable updater;
   private final ArrayList<Consumer> connectionStateListeners = new ArrayList<>();
   private Consumer receivedConsumer;

   public KryoAdapter(int tcpPort)
   {
      Server server = new Server();
      server.addListener(kryoListener);
      server.getKryo().setRegistrationRequired(false);
      isConnectedSupplier = () -> server.getConnections().length > 0;
      updater = () -> ExceptionTools.handle(() -> server.update(250), DefaultExceptionHandler.RUNTIME_EXCEPTION);
      connector = () -> kickstarter(() -> server.bind(tcpPort));
      disconnector = () -> server.close();
      tcpSender = message -> server.sendToAllTCP(message);
   }

   public KryoAdapter(String serverAddress, int tcpPort)
   {
      Client client = new Client();
      client.addListener(kryoListener);
      client.getKryo().setRegistrationRequired(false);
      isConnectedSupplier = () -> client.isConnected();
      updater = () -> ExceptionTools.handle(() -> client.update(250), DefaultExceptionHandler.RUNTIME_EXCEPTION);
      connector = () -> kickstarter(() -> client.connect(5000, serverAddress, tcpPort));
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
      connector.run();
   }

   public void disconnect()
   {
      disconnector.run();
   }

   public void update()
   {
      updater.run();
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

   /** Imagine having to kick a rusty lawn mower to start it, then you understand this method. */
   private void kickstarter(RunnableThatThrows connect)
   {
      new Thread(() -> {
         try
         {
            connect.run();
         }
         catch (Throwable e)
         {
            throw new RuntimeException(e);
         }
      }).start();
      new Thread(() -> {
         try
         {
            while (!isConnectedSupplier.getAsBoolean())
            {
               updater.run();
            }
         }
         catch (Throwable e)
         {
            throw new RuntimeException(e);
         }
      }).start();
   }
}
