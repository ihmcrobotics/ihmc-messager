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
   protected final MessagerAPI messagerAPI;

   protected final AtomicBoolean isConnected = new AtomicBoolean(false);
   protected final ConcurrentHashMap<Topic<?>, TopicEntry> topicEntries = new ConcurrentHashMap<>();
   protected final List<MessagerStateListener> connectionStateListeners = new ArrayList<>();

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

      TopicEntry topicEntry = topicEntries.get(messageTopic);

      if (topicEntry != null)
         topicEntry.consumeMessage(message);
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
      TopicEntry topicEntry = topicEntries.get(topic);
      if (topicEntry == null)
      {
         topicEntry = new TopicEntry();
         topicEntries.put(topic, topicEntry);
      }
      topicEntry.bindVariable((AtomicReference<Object>) input);
   }

   /** {@inheritDoc} */
   @SuppressWarnings("unchecked")
   @Override
   public <T> boolean removeInput(Topic<T> topic, AtomicReference<T> input)
   {
      TopicEntry topicEntry = topicEntries.get(topic);
      if (topicEntry == null)
         return false;
      return topicEntry.removeVariable((AtomicReference<Object>) input);
   }

   /** {@inheritDoc} */
   @Override
   @SuppressWarnings("unchecked")
   public <T> void addTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      TopicEntry topicEntry = topicEntries.get(topic);
      if (topicEntry == null)
      {
         topicEntry = new TopicEntry();
         topicEntries.put(topic, topicEntry);
      }
      topicEntry.addListener((TopicListener<Object>) listener);
   }

   /** {@inheritDoc} */
   @SuppressWarnings("unchecked")
   @Override
   public <T> boolean removeTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      TopicEntry topicEntry = topicEntries.get(topic);
      if (topicEntry == null)
         return false;
      return topicEntry.removeListener((TopicListener<Object>) listener);
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
   public void addMessagerStateListener(MessagerStateListener listener)
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

   protected static class TopicEntry
   {
      private final List<AtomicReference<Object>> boundVariables = new ArrayList<>();
      private final List<TopicListener<Object>> topicListeners = new ArrayList<>();
      private final List<TopicListenerSyncable<Object>> syncableTopicListeners = new ArrayList<>();

      protected void bindVariable(AtomicReference<Object> variable)
      {
         boundVariables.add(variable);
      }

      protected boolean removeVariable(AtomicReference<Object> variable)
      {
         return boundVariables.remove(variable);
      }

      protected void addListener(TopicListener<Object> listener)
      {
         if (listener instanceof TopicListenerSyncable<Object> syncListener)
            syncableTopicListeners.add(syncListener);
         else
            topicListeners.add(listener);
      }

      protected boolean removeListener(TopicListener<Object> listener)
      {
         if (listener instanceof TopicListenerSyncable<Object> syncListener)
            return syncableTopicListeners.remove(syncListener);
         else
            return topicListeners.remove(listener);
      }

      protected void consumeMessage(Message<?> message)
      {
         Object messageContent = message.getMessageContent();
         SynchronizeHint synchronizeHint = message.getSynchronizeHint();
         if (synchronizeHint == null)
            synchronizeHint = SynchronizeHint.NONE;

         for (int i = 0; i < boundVariables.size(); i++)
            boundVariables.get(i).set(messageContent);
         for (int i = 0; i < topicListeners.size(); i++)
            topicListeners.get(i).receivedMessageForTopic(messageContent);
         for (int i = 0; i < syncableTopicListeners.size(); i++)
            syncableTopicListeners.get(i).receivedMessageForTopic(messageContent, synchronizeHint);
      }
   }
}
