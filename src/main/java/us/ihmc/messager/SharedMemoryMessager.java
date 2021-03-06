package us.ihmc.messager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.log.LogTools;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;

/**
 * Implementation of {@code Messager} using shared memory.
 * 
 * @author Sylvain Bertrand
 */
public class SharedMemoryMessager implements Messager
{
   private final MessagerAPI messagerAPI;

   private final AtomicBoolean isConnected = new AtomicBoolean(false);
   private final ConcurrentHashMap<Topic<?>, List<AtomicReference<Object>>> boundVariables = new ConcurrentHashMap<>();
   private final ConcurrentHashMap<Topic<?>, List<TopicListener<Object>>> topicListenersMap = new ConcurrentHashMap<>();
   private final List<MessagerStateListener> connectionStateListeners = new ArrayList<>();

   /**
    * Creates a new messager.
    * 
    * @param messagerAPI the API to use with this messager.
    */
   public SharedMemoryMessager(MessagerAPI messagerAPI)
   {
      this.messagerAPI = messagerAPI;
   }

   /** {@inheritDoc} */
   @Override
   public <T> void submitMessage(Message<T> message)
   {
      if (!messagerAPI.containsTopic(message.getTopicID()))
         throw new RuntimeException("The message is not part of this messager's API.");

      Topic<?> messageTopic = messagerAPI.findTopic(message.getTopicID());

      if (!isConnected.get())
      {
         LogTools.warn("This messager is closed, message's topic: " + messageTopic.getSimpleName());
         return;
      }

      List<AtomicReference<Object>> boundVariablesForTopic = boundVariables.get(messageTopic);
      if (boundVariablesForTopic != null)
         boundVariablesForTopic.forEach(variable -> variable.set(message.getMessageContent()));

      List<TopicListener<Object>> topicListeners = topicListenersMap.get(messageTopic);
      if (topicListeners != null)
         topicListeners.forEach(listener -> listener.receivedMessageForTopic(message.getMessageContent()));
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public <T> AtomicReference<T> createInput(Topic<T> topic, T defaultValue)
   {
      AtomicReference<T> boundVariable = new AtomicReference<>(defaultValue);

      List<AtomicReference<Object>> boundVariablesForTopic = boundVariables.get(topic);
      if (boundVariablesForTopic == null)
      {
         boundVariablesForTopic = new ArrayList<>();
         boundVariables.put(topic, boundVariablesForTopic);
      }
      boundVariablesForTopic.add((AtomicReference<Object>) boundVariable);
      return boundVariable;
   }

   /** {@inheritDoc} */
   @Override
   public <T> boolean removeInput(Topic<T> topic, AtomicReference<T> input)
   {
      List<?> boundVariablesForTopic = boundVariables.get(topic);
      if (boundVariablesForTopic == null)
         return false;
      else
         return boundVariablesForTopic.remove(input);
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
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

   /** {@inheritDoc} */
   @Override
   public <T> boolean removeTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      List<?> topicListeners = topicListenersMap.get(topic);
      if (topicListeners == null)
         return false;
      else
         return topicListeners.remove(listener);
   }

   /** {@inheritDoc} */
   @Override
   public void startMessager()
   {
      isConnected.set(true);
      notifyMessagerStateListeners();
   }

   /** {@inheritDoc} */
   @Override
   public void closeMessager()
   {
      isConnected.set(false);
      notifyMessagerStateListeners();
   }

   /** {@inheritDoc} */
   @Override
   public boolean isMessagerOpen()
   {
      return isConnected.get();
   }

   /** {@inheritDoc} */
   @Override
   public void registerMessagerStateListener(MessagerStateListener listener)
   {
      connectionStateListeners.add(listener);
   }

   /** {@inheritDoc} */
   @Override
   public boolean removeMessagerStateListener(MessagerStateListener listener)
   {
      return connectionStateListeners.remove(listener);
   }

   /** {@inheritDoc} */
   @Override
   public void notifyMessagerStateListeners()
   {
      connectionStateListeners.forEach(listener -> listener.messagerStateChanged(isMessagerOpen()));
   }

   /** {@inheritDoc} */
   @Override
   public MessagerAPI getMessagerAPI()
   {
      return messagerAPI;
   }
}
