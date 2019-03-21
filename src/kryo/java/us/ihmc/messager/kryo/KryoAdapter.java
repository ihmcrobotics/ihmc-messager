package us.ihmc.messager.kryo;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import org.apache.commons.lang3.mutable.MutableBoolean;
import us.ihmc.commons.RunnableThatThrows;
import us.ihmc.commons.exception.DefaultExceptionHandler;
import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.log.LogTools;

import java.util.ArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * <p>Unifies the API of Kryonet Server and Client. Users should aim to create one server and one client.</p>
 *
 * <p>Uses lamdas and callbacks pretty heavily since Kryonet Client and Server do not share any interfaces.</p>
 */
public class KryoAdapter
{
   /** Default write size buffer. */
   public static final int DEFAULT_WRITE_BUFFER_SIZE = 16384;
   /** Default object size buffer. */
   public static final int DEFAULT_OBJECT_BUFFER_SIZE = 8192;
   private Listener kryoListener = new KryoListener();
   private Consumer receivedConsumer;
   private final ArrayList<Consumer> connectionStateListeners = new ArrayList<>();

   private final BooleanSupplier isConnectedSupplier;
   private final RunnableThatThrows updater;
   private final RunnableThatThrows connector;
   private final RunnableThatThrows disconnector;
   private final Consumer tcpSender;

   /**
    * Create a Kryonet server with default buffer sizes.
    *
    * @param tcpPort
    * @return server
    */
   public static KryoAdapter createServer(int tcpPort)
   {
      return new KryoAdapter(tcpPort, DEFAULT_WRITE_BUFFER_SIZE, DEFAULT_OBJECT_BUFFER_SIZE);
   }

   /**
    * Create a Kryonet server.
    *
    * @param tcpPort
    * @param writeBufferSize One buffer of this size is allocated for each connected client. Objects are serialized to the write
    *           buffer where the bytes are queued until they can be written to the TCP socket.
    *           <p>
    *           Normally the socket is writable and the bytes are written immediately. If the socket cannot be written to and
    *           enough serialized objects are queued to overflow the buffer, then the connection will be closed.
    *           <p>
    *           The write buffer should be sized at least as large as the largest object that will be sent, plus some head room to
    *           allow for some serialized objects to be queued in case the buffer is temporarily not writable. The amount of head
    *           room needed is dependent upon the size of objects being sent and how often they are sent.
    * @param objectBufferSize One (using only TCP) or three (using both TCP and UDP) buffers of this size are allocated. These
    *           buffers are used to hold the bytes for a single object graph until it can be sent over the network or
    *           deserialized.
    *           <p>
    *           The object buffers should be sized at least as large as the largest object that will be sent or received.
    * @return server
    */
   public static KryoAdapter createServer(int tcpPort, int writeBufferSize, int objectBufferSize)
   {
      return new KryoAdapter(tcpPort, writeBufferSize, objectBufferSize);
   }

   /**
    * Create a Kryonet client with default buffer sizes.
    *
    * @param serverAddress
    * @param tcpPort
    * @return client
    */
   public static KryoAdapter createClient(String serverAddress, int tcpPort)
   {
      return new KryoAdapter(serverAddress, tcpPort, DEFAULT_WRITE_BUFFER_SIZE, DEFAULT_OBJECT_BUFFER_SIZE);
   }

   /**
    * Create a Kryonet client.
    *
    * @param serverAddress
    * @param tcpPort
    * @param writeBufferSize One buffer of this size is allocated. Objects are serialized to the write buffer where the bytes are
    *           queued until they can be written to the TCP socket.
    *           <p>
    *           Normally the socket is writable and the bytes are written immediately. If the socket cannot be written to and
    *           enough serialized objects are queued to overflow the buffer, then the connection will be closed.
    *           <p>
    *           The write buffer should be sized at least as large as the largest object that will be sent, plus some head room to
    *           allow for some serialized objects to be queued in case the buffer is temporarily not writable. The amount of head
    *           room needed is dependent upon the size of objects being sent and how often they are sent.
    * @param objectBufferSize One (using only TCP) or three (using both TCP and UDP) buffers of this size are allocated. These
    *           buffers are used to hold the bytes for a single object graph until it can be sent over the network or
    *           deserialized.
    *           <p>
    *           The object buffers should be sized at least as large as the largest object that will be sent or received.
    * @return client
    */
   public static KryoAdapter createClient(String serverAddress, int tcpPort, int writeBufferSize, int objectBufferSize)
   {
      return new KryoAdapter(serverAddress, tcpPort, writeBufferSize, objectBufferSize);
   }

   private KryoAdapter(int tcpPort, int writeBufferSize, int objectBufferSize)
   {
      Server server = new Server(writeBufferSize, objectBufferSize);
      server.addListener(kryoListener);
      server.getKryo().setRegistrationRequired(false);
      isConnectedSupplier = () -> server.getConnections().length > 0;
      updater = () -> server.update(250);
      connector = () -> server.bind(tcpPort);
      disconnector = () -> server.close();
      tcpSender = message -> server.sendToAllTCP(message);
   }

   private KryoAdapter(String serverAddress, int tcpPort, int writeBufferSize, int objectBufferSize)
   {
      Client client = new Client(writeBufferSize, objectBufferSize);
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

   /**
    * Connect across to an expected instance of Kryo that is
    * of the opposite type. If you created a server, you need to connect
    * to a client.
    */
   public void connect()
   {
      new Thread(() -> startNonBlockingConnect()).start();  // this is the "kickstart" method required to
      new Thread(() -> waitForConnection()).start();        // get Kryo to connect
   }

   private void startNonBlockingConnect()
   {
      LogTools.debug("Connecting...");
      MutableBoolean successful = new MutableBoolean(false);
      while (!successful.getValue())
      {
         successful.setTrue();
         ExceptionTools.handle(() -> connector.run(), e ->
         {
            LogTools.error(e.getMessage());
            LogTools.debug("Trying to connect again...");
            successful.setFalse();
         });
      }
   }

   private void waitForConnection()
   {
      while (!isConnectedSupplier.getAsBoolean())
      {
         LogTools.debug("Updating...");
         ExceptionTools.handle(() -> updater.run(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
      }
   }

   /**
    * Closes all open connections and the server port(s) if applicable.
    * Doesn't seem to free up much memory.
    */
   public void disconnect()
   {
      ExceptionTools.handle(() -> disconnector.run(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
   }

   /**
    * <p>Update that must be called to receive any data and, for a server, to accept new connections.</p>
    *
    * <p>For a server: Accepts any new connections and reads or writes any pending data for the current connections.
    * Wait for up to 250 milliseconds for a connection to be ready to process. May be zero to return
    * immediately if there are no connections to process.</p><br>
    *
    * <p>For a client: Reads or writes any pending data for this client. Multiple threads should not call this method at the same time.
    * Wait for up to 250 milliseconds for data to be ready to process. May be zero to return immediately
    * if there is no data to process.</p>
    */
   public void update()
   {
      ExceptionTools.handle(() -> updater.run(), DefaultExceptionHandler.RUNTIME_EXCEPTION);
   }

   /**
    * Serializes and sends the object over the network using TCP. Non-blocking.
    *
    * @param object to send
    */
   public void sendTCP(Object object)
   {
      tcpSender.accept(object);
   }

   /**
    * If this adapter is connected to a server or client.
    *
    * @return connected
    */
   public boolean isConnected()
   {
      return isConnectedSupplier.getAsBoolean();
   }

   /**
    * Subscribe to received messages.
    *
    * @param receivedConsumer
    */
   public void setReceivedListener(Consumer receivedConsumer)
   {
      this.receivedConsumer = receivedConsumer;
   }

   /**
    * Add a connection state listener. Will callback on connected and disconnected events.
    *
    * @param connectionStateListener
    */
   public void addConnectionStateListener(Consumer<Boolean> connectionStateListener)
   {
      connectionStateListeners.add(connectionStateListener);
   }
}
