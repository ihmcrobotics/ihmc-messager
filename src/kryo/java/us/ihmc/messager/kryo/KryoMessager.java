package us.ihmc.messager.kryo;

import us.ihmc.log.LogTools;
import us.ihmc.messager.Message;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.messager.MessagerStateListener;
import us.ihmc.messager.TopicListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class KryoMessager implements Messager
{
   private final MessagerAPI messagerAPI;
   private final KryoAdapter kryoAdapter;
   private MessagerUpdateThread messagerUpdateThread;

   private final ConcurrentHashMap<Topic<?>, List<AtomicReference<Object>>> inputVariablesMap = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Topic<?>, List<TopicListener<Object>>> topicListenersMap = new ConcurrentHashMap<>();
   private final List<MessagerStateListener> connectionStateListeners = new ArrayList<>();

   private boolean allowSelfSubmit = true;

   /** Creates server. */
   public static KryoMessager createServer(MessagerAPI messagerAPI, int tcpPort, String name, int updatePeriodMillis)
   {
      return new KryoMessager(messagerAPI, KryoAdapter.createServer(tcpPort), new DefaultMessagerUpdateThread(name, updatePeriodMillis));
   }

   /** Creates server. */
   public static KryoMessager createServer(MessagerAPI messagerAPI, int tcpPort, MessagerUpdateThread messagerUpdateThread)
   {
      return new KryoMessager(messagerAPI, KryoAdapter.createServer(tcpPort), messagerUpdateThread);
   }

   /** Creates client. */
   public static KryoMessager createClient(MessagerAPI messagerAPI, String serverAddress, int tcpPort, String name, int updatePeriodMillis)
   {
      return new KryoMessager(messagerAPI, KryoAdapter.createClient(serverAddress, tcpPort), new DefaultMessagerUpdateThread(name, updatePeriodMillis));
   }

   /** Creates client. */
   public static KryoMessager createClient(MessagerAPI messagerAPI, String serverAddress, int tcpPort, MessagerUpdateThread messagerUpdateThread)
   {
      return new KryoMessager(messagerAPI, KryoAdapter.createClient(serverAddress, tcpPort), messagerUpdateThread);
   }

   private KryoMessager(MessagerAPI messagerAPI, KryoAdapter kryoAdapter, MessagerUpdateThread messagerUpdateThread)
   {
      this.messagerAPI = messagerAPI;
      this.kryoAdapter = kryoAdapter;
      this.messagerUpdateThread = messagerUpdateThread;

      kryoAdapter.setRecievedListener(object -> receiveMessage(object));
   }

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
         LogTools.warn("This messager is closed, message's topic: " + messageTopic.getName());
         return;
      }

      LogTools.debug("Submit message for topic: {}", messageTopic.getName());

      kryoAdapter.sendTCP(message);
   }

   private void receiveMessage(Object object)
   {
      if (object == null || !(object instanceof Message))
         return;

      Message message = (Message) object;

      if (!messagerAPI.containsTopic(message.getTopicID()))
         throw new RuntimeException("The message is not part of this messager's API.");

      Topic messageTopic = messagerAPI.findTopic(message.getTopicID());

      LogTools.debug("Packet received from network with message name: {}", messageTopic.getName());

      List<AtomicReference<Object>> inputVariablesForTopic = inputVariablesMap.get(messageTopic);
      if (inputVariablesForTopic != null)
         inputVariablesForTopic.forEach(variable -> variable.set(message.getMessageContent()));

      List<TopicListener<Object>> topicListeners = topicListenersMap.get(messageTopic);
      if (topicListeners != null)
         topicListeners.forEach(listener -> listener.receivedMessageForTopic(message.getMessageContent()));
   }

   @Override
   public <T> AtomicReference<T> createInput(Topic<T> topic, T defaultValue)
   {
      AtomicReference<T> boundVariable = new AtomicReference<>(defaultValue);

      List<AtomicReference<Object>> boundVariablesForTopic = inputVariablesMap.get(topic);
      if (boundVariablesForTopic == null)
      {
         boundVariablesForTopic = new ArrayList<>();
         inputVariablesMap.put(topic, boundVariablesForTopic);
      }
      boundVariablesForTopic.add((AtomicReference<Object>) boundVariable);
      return boundVariable;
   }

   @Override
   public <T> void registerTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      List<TopicListener<Object>> topicListeners = topicListenersMap.get(topic);
      if (topicListeners == null)
      {
         topicListeners = new ArrayList<>();
         topicListenersMap.put(topic, topicListeners);
      }
      topicListeners.add((TopicListener<Object>) listener);
   }

   @Override
   public void startMessager() throws Exception
   {
      LogTools.debug("Connecting kryo");
      kryoAdapter.connect();
      LogTools.debug("Starting kryo update");
      messagerUpdateThread.start(() -> kryoAdapter.update());
   }

   @Override
   public void closeMessager() throws Exception
   {
      kryoAdapter.disconnect();
      messagerUpdateThread.stop();
   }

   @Override
   public boolean isMessagerOpen()
   {
      return kryoAdapter.isConnected();
   }

   @Override
   public void notifyMessagerStateListeners()
   {
      connectionStateListeners.forEach(listener -> listener.messagerStateChanged(isMessagerOpen()));
   }

   @Override
   public void registerMessagerStateListener(MessagerStateListener listener)
   {
      kryoAdapter.addConnectionStateListener(state -> listener.messagerStateChanged(state));
   }

   @Override
   public MessagerAPI getMessagerAPI()
   {
      return messagerAPI;
   }
}
