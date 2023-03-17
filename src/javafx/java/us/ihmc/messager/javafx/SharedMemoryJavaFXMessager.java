package us.ihmc.messager.javafx;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.lang3.mutable.MutableBoolean;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
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
   private final Map<Topic<?>, JavaFXSyncedTopicListeners> javaFXSyncedTopicListeners = new HashMap<>();
   private final AnimationTimer animationTimer;
   private boolean readingListeners = false;

   /**
    * Creates a new messager.
    *
    * @param messagerAPI the API to use with this messager.
    */
   public SharedMemoryJavaFXMessager(MessagerAPI messagerAPI)
   {
      super(messagerAPI);
      animationTimer = new AnimationTimer()
      {
         @Override
         public void handle(long now)
         {
            try
            {
               readingListeners = true;

               javaFXSyncedTopicListeners.entrySet().removeIf(entry -> entry.getValue().isEmpty());

               for (JavaFXSyncedTopicListeners listener : javaFXSyncedTopicListeners.values())
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
      };
   }

   /** {@inheritDoc} */
   @Override
   public <T> void addFXTopicListener(Topic<T> topic, TopicListener<T> listener)
   {
      JavaFXSyncedTopicListeners topicListeners = javaFXSyncedTopicListeners.get(topic);

      if (topicListeners != null)
      {
         topicListeners.addListener(listener);
         return;
      }

      if (!readingListeners && Platform.isFxApplicationThread())
      { // It appears to not be enough to check for application thread somehow.
         topicListeners = new JavaFXSyncedTopicListeners(topic);
         javaFXSyncedTopicListeners.put(topic, topicListeners);
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
            topicListeners = new JavaFXSyncedTopicListeners(topic);
            javaFXSyncedTopicListeners.put(topic, topicListeners);
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
      JavaFXSyncedTopicListeners topicListeners = javaFXSyncedTopicListeners.get(topic);
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
      animationTimer.start();
   }

   /** {@inheritDoc} */
   @Override
   public void closeMessager()
   {
      javaFXSyncedTopicListeners.values().forEach(JavaFXSyncedTopicListeners::dispose);
      javaFXSyncedTopicListeners.clear();
      super.closeMessager();
      animationTimer.stop();
   }

   @SuppressWarnings("unchecked")
   private class JavaFXSyncedTopicListeners
   {
      private static final Object NULL_OBJECT = new Object();
      private final ConcurrentLinkedQueue<Object> inputQueue = new ConcurrentLinkedQueue<>();
      private final ConcurrentLinkedQueue<TopicListener<Object>> listeners = new ConcurrentLinkedQueue<>();

      private JavaFXSyncedTopicListeners(Topic<?> topic)
      {
         addTopicListener(topic, message ->
         {
            if (message != null)
               inputQueue.add(message);
            else
               inputQueue.add(NULL_OBJECT);
         });
      }

      private void addListener(TopicListener<?> listener)
      {
         listeners.add((TopicListener<Object>) listener);
      }

      private boolean removeListener(TopicListener<?> listener)
      {
         return listeners.remove(listener);
      }

      private void notifyListeners()
      {
         while (!inputQueue.isEmpty())
         {
            Object newData = inputQueue.poll();
            if (newData == NULL_OBJECT)
               listeners.forEach(listener -> listener.receivedMessageForTopic(null));
            else
               listeners.forEach(listener -> listener.receivedMessageForTopic(newData));
         }
      }

      public boolean isEmpty()
      {
         return listeners.isEmpty();
      }

      public void dispose()
      {
         inputQueue.clear();
         listeners.clear();
      }
   }
}
