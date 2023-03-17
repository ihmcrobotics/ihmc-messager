package us.ihmc.messager.kryo;

import static us.ihmc.commons.exception.DefaultExceptionHandler.RUNTIME_EXCEPTION;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import us.ihmc.commons.exception.ExceptionTools;
import us.ihmc.commons.thread.ThreadTools;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Message;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.messager.MessagerStateListener;
import us.ihmc.messager.TopicListener;

/**
 * A {@link Messager} implementation that uses Kryonet under the hood. With Kryo there must be a
 * server and a client, so {@link KryoMessager#createServer} needs to be called on one side and
 * {@link KryoMessager#createClient} on the other. Sometimes the requested port is unavailable and
 * you will need to select another.
 */
public class KryoMessager implements Messager
{
   /** The Messager API */
   private final MessagerAPI messagerAPI;
   /** Access to Kryonet */
   private final KryoAdapter kryoAdapter;
   /** Abstraction for external threads to update this how they want */
   private MessagerUpdateThread messagerUpdateThread;

   private final ConcurrentHashMap<Topic<?>, List<AtomicReference<Object>>> inputVariablesMap = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Topic<?>, List<TopicListener<Object>>> topicListenersMap = new ConcurrentHashMap<>();
   private final Map<MessagerStateListener, Consumer<Boolean>> connectionStateListeners = new HashMap<>();

   private boolean allowSelfSubmit = true;

   /**
    * Creates a KryoMessager server side using
    * {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} under the
    * hood.
    *
    * @param messagerAPI        the Messager API
    * @param tcpPort            to host the server on
    * @param name               of the update thread
    * @param updatePeriodMillis how often to update this messager
    * @return new Kryo Messager
    */
   public static KryoMessager createServer(MessagerAPI messagerAPI, int tcpPort, String name, int updatePeriodMillis)
   {
      return new KryoMessager(messagerAPI, KryoAdapter.createServer(tcpPort), new DefaultMessagerUpdateThread(name, updatePeriodMillis));
   }

   /**
    * Creates a KryoMessager server that provides the user with a Runnable through
    * {@link MessagerUpdateThread} that updates the Kryo internals. The user is responsible for calling
    * that runnable periodically.
    *
    * @param messagerAPI          the Messager API
    * @param tcpPort              to host the server on
    * @param messagerUpdateThread for using your own thread scheduler or for manual calls for testing
    * @return new Kryo Messager
    */
   public static KryoMessager createServer(MessagerAPI messagerAPI, int tcpPort, MessagerUpdateThread messagerUpdateThread)
   {
      return new KryoMessager(messagerAPI, KryoAdapter.createServer(tcpPort), messagerUpdateThread);
   }

   /**
    * Creates a KryoMessager client side using
    * {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)} under the
    * hood. The client side requires an address i.e. "localhost" or "192.168.1.3", etc.
    *
    * @param messagerAPI        the Messager API
    * @param serverAddress      of the host to connect to, an IP address or domain
    * @param tcpPort            port that the server is bound to
    * @param name               of the update thread
    * @param updatePeriodMillis how often to update this messager
    * @return new Kryo Messager
    */
   public static KryoMessager createClient(MessagerAPI messagerAPI, String serverAddress, int tcpPort, String name, int updatePeriodMillis)
   {
      return new KryoMessager(messagerAPI, KryoAdapter.createClient(serverAddress, tcpPort), new DefaultMessagerUpdateThread(name, updatePeriodMillis));
   }

   /**
    * Creates a KryoMessager client that provides the user with a Runnable through
    * {@link MessagerUpdateThread} that updates the Kryo internals. The user is responsible for calling
    * that runnable periodically.
    *
    * @param messagerAPI          the Messager API
    * @param serverAddress        of the host to connect to, an IP address or domain
    * @param tcpPort              port that the server is bound to
    * @param messagerUpdateThread for using your own thread scheduler or for manual calls for testing
    * @return new Kryo Messager
    */
   public static KryoMessager createClient(MessagerAPI messagerAPI, String serverAddress, int tcpPort, MessagerUpdateThread messagerUpdateThread)
   {
      return new KryoMessager(messagerAPI, KryoAdapter.createClient(serverAddress, tcpPort), messagerUpdateThread);
   }

   private KryoMessager(MessagerAPI messagerAPI, KryoAdapter kryoAdapter, MessagerUpdateThread messagerUpdateThread)
   {
      this.messagerAPI = messagerAPI;
      this.kryoAdapter = kryoAdapter;
      this.messagerUpdateThread = messagerUpdateThread;

      kryoAdapter.setReceivedListener(this::receiveMessage);
   }

   /** {@inheritDoc} */
   @Override
   public <T> void submitMessage(Message<T> message)
   {
      if (!messagerAPI.containsTopic(message.getTopicID()))
         throw new RuntimeException("The message is not part of this messager's API.");

      Topic<?> messageTopic = messagerAPI.findTopic(message.getTopicID());

      if (allowSelfSubmit)
         receiveMessage(message);

      if (!kryoAdapter.isConnected())
      {
         LogTools.warn(1, "This messager is closed, message's topic: " + messageTopic.getName());
         return;
      }

      LogTools.trace("Submit message for topic: {}", messageTopic.getName());

      kryoAdapter.sendTCP(message);
   }

   @SuppressWarnings("rawtypes")
   private void receiveMessage(Object object)
   {
      if (!(object instanceof Message))
         return;

      Message message = (Message) object;

      if (!messagerAPI.containsTopic(message.getTopicID()))
         throw new RuntimeException("The message is not part of this messager's API.");

      Topic messageTopic = messagerAPI.findTopic(message.getTopicID());

      LogTools.trace("Packet received from network with message name: {}", messageTopic.getName());

      List<AtomicReference<Object>> inputVariablesForTopic = inputVariablesMap.get(messageTopic);
      if (inputVariablesForTopic != null)
         inputVariablesForTopic.forEach(variable -> variable.set(message.getMessageContent()));

      List<TopicListener<Object>> topicListeners = topicListenersMap.get(messageTopic);
      if (topicListeners != null)
         topicListeners.forEach(listener -> listener.receivedMessageForTopic(message.getMessageContent()));
   }

   /** {@inheritDoc} */
   @Override
   public <T> AtomicReference<T> createInput(Topic<T> topic, T defaultValue)
   {
      AtomicReference<T> boundVariable = new AtomicReference<>(defaultValue);
      attachInput(topic, boundVariable);
      return boundVariable;
   }

   /** {@inheritDoc} */
   @SuppressWarnings("unchecked")
   @Override
   public <T> void attachInput(Topic<T> topic, AtomicReference<T> input)
   {
      List<AtomicReference<Object>> boundVariablesForTopic = inputVariablesMap.computeIfAbsent(topic, k -> new ArrayList<>());
      boundVariablesForTopic.add((AtomicReference<Object>) input);
   }

   /** {@inheritDoc} */
   @Override
   public <T> boolean removeInput(Topic<T> topic, AtomicReference<T> input)
   {
      List<AtomicReference<Object>> boundVariablesForTopic = inputVariablesMap.get(topic);
      if (boundVariablesForTopic == null)
         return false;
      else
         return boundVariablesForTopic.remove(input);
   }

   /** {@inheritDoc} */
   @SuppressWarnings("unchecked")
   @Override
   public <T> void addTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      List<TopicListener<Object>> topicListeners = topicListenersMap.computeIfAbsent(topic, k -> new ArrayList<>());
      topicListeners.add((TopicListener<Object>) listener);
   }

   /** {@inheritDoc} */
   @Override
   public <T> boolean removeTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      List<TopicListener<Object>> topicListeners = topicListenersMap.get(topic);
      if (topicListeners == null)
         return false;
      else
         return topicListeners.remove(listener);
   }

   /**
    * Starts the messager, blocking until it's started.
    *
    * {@inheritDoc}
    */
   @Override
   public void startMessager() throws Exception
   {
      startMessagerBlocking();
   }

   /**
    * Starts the messager, blocking until it's started.
    */
   public void startMessagerBlocking()
   {
      LogTools.debug("Starting to connect KryoNet");
      kryoAdapter.connect();

      LogTools.debug("Waiting for KryoNet to connect");
      while (!isMessagerOpen()) // this is necessary before starting the messager update thread
      { // otherwise connection times out because multiple threads are calling
         Thread.yield(); // kryo.update()
      }

      LogTools.debug("Starting KryoNet update thread");
      messagerUpdateThread.start(kryoAdapter::update);
   }

   /**
    * Starts the messager asyncronously. This may cause initial bugs if your application does not wait for startup.
    */
   public void startMessagerAsyncronously()
   {
      ThreadTools.startAThread(() -> ExceptionTools.handle(this::startMessager, RUNTIME_EXCEPTION), "KryoMessagerAsyncConnectionThread");
   }

   /** {@inheritDoc} */
   @Override
   public void closeMessager() throws Exception
   {
      kryoAdapter.disconnect();
      messagerUpdateThread.stop();
   }

   /** {@inheritDoc} */
   @Override
   public boolean isMessagerOpen()
   {
      return kryoAdapter.isConnected();
   }

   /** {@inheritDoc} */
   @Override
   public void notifyMessagerStateListeners()
   {
      connectionStateListeners.keySet().forEach(listener -> listener.messagerStateChanged(isMessagerOpen()));
   }

   /** {@inheritDoc} */
   @Override
   public void addMessagerStateListener(MessagerStateListener listener)
   {
      Consumer<Boolean> kryoListener = listener::messagerStateChanged;
      connectionStateListeners.put(listener, kryoListener);
      kryoAdapter.addConnectionStateListener(listener::messagerStateChanged);
   }

   @Override
   public boolean removeMessagerStateListener(MessagerStateListener listener)
   {
      Consumer<Boolean> kryoListener = connectionStateListeners.remove(listener);
      if (kryoListener == null)
      {
         return false;
      }
      else
      {
         kryoAdapter.removeConnectionStateListener(kryoListener);
         return true;
      }
   }

   /** {@inheritDoc} */
   @Override
   public MessagerAPI getMessagerAPI()
   {
      return messagerAPI;
   }
}
