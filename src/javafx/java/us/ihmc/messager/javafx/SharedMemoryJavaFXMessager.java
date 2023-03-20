package us.ihmc.messager.javafx;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.mutable.MutableBoolean;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import us.ihmc.messager.Message;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.messager.SharedMemoryMessager;
import us.ihmc.messager.TopicListener;

/**
 * Implementation of {@code JavaFXMessager} using shared memory.
 *
 * @author Sylvain Bertrand
 */
public class SharedMemoryJavaFXMessager extends SharedMemoryMessager implements JavaFXMessager
{
   private final Map<Topic<?>, BufferedTopicListeners> bufferedTopicListeners = new HashMap<>();
   private final AnimationTimer animationTimer;
   private boolean readingListeners = false;

   /**
    * Creates a new messager.
    *
    * @param messagerAPI the API to use with this messager.
    */
   public SharedMemoryJavaFXMessager(MessagerAPI messagerAPI)
   {
      this(messagerAPI, false);
   }

   /**
    * Creates a new messager.
    *
    * @param messagerAPI the API to use with this messager.
    */
   public SharedMemoryJavaFXMessager(MessagerAPI messagerAPI, boolean managed)
   {
      super(messagerAPI);
      if (managed)
      {
         animationTimer = null;
      }
      else
      {
         animationTimer = new AnimationTimer()
         {
            @Override
            public void handle(long now)
            {
               updateFXTopicListeners();
            }
         };
      }

   }

   public void updateFXTopicListeners()
   {
      try
      {
         readingListeners = true;

         bufferedTopicListeners.entrySet().removeIf(entry -> entry.getValue().isEmpty());

         for (BufferedTopicListeners listener : bufferedTopicListeners.values())
            listener.notifyListeners();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         readingListeners = false;
      }
   }

   /** {@inheritDoc} */
   @Override
   public <T> void submitMessage(Message<T> message)
   {
      super.submitMessage(message);
      BufferedTopicListeners topicListeners = bufferedTopicListeners.get(message.getTopic(messagerAPI));
      topicListeners.submitMessage(message);
   }

   /** {@inheritDoc} */
   @Override
   public <T> void addFXTopicListener(Topic<T> topic, FXTopicListener<T> listener)
   {
      addFXTopicListener(topic, (TopicListener<T>) listener);
   }

   /** {@inheritDoc} */
   @Override
   public <T> void addFXTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      BufferedTopicListeners topicListeners = bufferedTopicListeners.get(topic);

      if (topicListeners != null)
      {
         topicListeners.addListener(listener);
         return;
      }

      if (!readingListeners && Platform.isFxApplicationThread())
      { // It appears to not be enough to check for application thread somehow.
         topicListeners = new BufferedTopicListeners(topic);
         bufferedTopicListeners.put(topic, topicListeners);
         topicListeners.addListener(listener);
      }
      else // The following one can throw an exception if the JavaFX thread has not started yet.
      {
         try
         { // Postpone the entire registration in case JavaFXSyncedTopicListeners has been created by another caller.
            runFXLater(() -> addFXTopicListener(topic, listener));
            return;
         }
         catch (IllegalStateException e)
         { // The JavaFX thread has not started yet, no need to invoke Platform.runLater(...).
            topicListeners = new BufferedTopicListeners(topic);
            bufferedTopicListeners.put(topic, topicListeners);
            topicListeners.addListener(listener);
         }
      }
   }

   protected void runFXLater(Runnable fxTask)
   {
      Platform.runLater(fxTask);
   }

   protected void runFXAndWait(final Runnable fxTask)
   {
      if (Platform.isFxApplicationThread())
      {
         tryRun(fxTask);
      }
      else
      {
         final CountDownLatch doneLatch = new CountDownLatch(1);

         runFXLater(() ->
         {
            tryRun(fxTask);
            doneLatch.countDown();
         });

         try
         {
            doneLatch.await();
         }
         catch (InterruptedException ex)
         {
            ex.printStackTrace();
         }
      }
   }

   protected void tryRun(Runnable fxTask)
   {
      try
      {
         fxTask.run();
      }
      catch (Throwable t)
      {
         System.err.println("Exception in fxTask");
         t.printStackTrace();
      }
   }

   /** {@inheritDoc} */
   @Override
   public <T> boolean removeFXTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      BufferedTopicListeners topicListeners = bufferedTopicListeners.get(topic);
      if (topicListeners == null)
         return false;

      if (!readingListeners && Platform.isFxApplicationThread())
      {
         return topicListeners.removeListener(listener);
      }
      else
      {
         try
         { // The following one can throw an exception if the JavaFX thread has not started yet.
            MutableBoolean result = new MutableBoolean();
            runFXAndWait(() -> result.setValue(topicListeners.removeListener(listener)));
            return result.booleanValue();
         }
         catch (IllegalStateException e)
         { // The JavaFX thread has not started yet, no need to invoke Platform.runLater(...).
            return topicListeners.removeListener(listener);
         }
      }
   }

   /** {@inheritDoc} */
   @Override
   public void startMessager()
   {
      super.startMessager();
      if (animationTimer != null)
         animationTimer.start();
   }

   /** {@inheritDoc} */
   @Override
   public void closeMessager()
   {
      bufferedTopicListeners.values().forEach(BufferedTopicListeners::dispose);
      bufferedTopicListeners.clear();
      super.closeMessager();
      if (animationTimer != null)
         animationTimer.stop();
   }

   @SuppressWarnings("unchecked")
   protected class BufferedTopicListeners
   {
      protected static final Object NULL_OBJECT = new Object();
      protected final ConcurrentLinkedQueue<Message<?>> messageQueue = new ConcurrentLinkedQueue<>();
      protected final ConcurrentLinkedQueue<TopicListener<Object>> listeners = new ConcurrentLinkedQueue<>();

      protected BufferedTopicListeners(Topic<?> topic)
      {
      }

      protected void submitMessage(Message<?> message)
      {
         if (message.getAuxiliaryData() == SynchronizeHint.SYNCHRONOUS)
         {
            runFXAndWait(() ->
            {
               Object newData = message.getMessageContent() == NULL_OBJECT ? null : message.getMessageContent();
               listeners.forEach(listener ->
               {
                  if (listener instanceof FXTopicListener<Object> fxListener)
                     fxListener.receivedMessageForFXTopic(newData, SynchronizeHint.SYNCHRONOUS);
                  else
                     listener.receivedMessageForTopic(newData);
               });
            });
         }
         else
         {
            messageQueue.add(message);
         }
      }

      protected void addListener(TopicListener<?> listener)
      {
         listeners.add((TopicListener<Object>) listener);
      }

      protected boolean removeListener(TopicListener<?> listener)
      {
         return listeners.remove(listener);
      }

      protected void notifyListeners()
      {
         while (!messageQueue.isEmpty())
         {
            Message<?> newMessage = messageQueue.poll();
            Object newData = newMessage.getMessageContent() == NULL_OBJECT ? null : newMessage.getMessageContent();
            SynchronizeHint newHint = newMessage.getAuxiliaryData() == null ? SynchronizeHint.NONE : (SynchronizeHint) newMessage.getAuxiliaryData();

            listeners.forEach(listener ->
            {
               if (listener instanceof FXTopicListener<Object> fxListener)
                  fxListener.receivedMessageForFXTopic(newData, newHint);
               else
                  listener.receivedMessageForTopic(newData);
            });
         }
      }

      protected boolean isEmpty()
      {
         return listeners.isEmpty();
      }

      protected void dispose()
      {
         messageQueue.clear();
         listeners.clear();
      }
   }
}
