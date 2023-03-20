package us.ihmc.messager.javafx;

import us.ihmc.messager.TopicListener;

/**
 * Listener intended for JavaFX topics which allows to pass an additional flag to indicate whether
 * the listener should operate asynchronous or synchronous.
 * 
 * @author Sylvain Bertrand
 * @param <T> the data type.
 */
public interface FXTopicListener<T> extends TopicListener<T>
{
   @Override
   default void receivedMessageForTopic(T messageContent)
   {
      receivedMessageForFXTopic(messageContent, SynchronizeHint.NONE);
   }

   /**
    * The messager just received data for the topic.
    * 
    * @param messageContent the data.
    * @param hint           hint for the execution should be performed: {@link SynchronizeHint#NONE}
    *                       nothing is expected, {@link SynchronizeHint#ASYNCHRONOUS} the listener
    *                       should return as soon as possible and the actual execution should be
    *                       performed asynchronously, {@link SynchronizeHint#SYNCHRONOUS} the listener
    *                       should return only once the execution is done.
    */
   void receivedMessageForFXTopic(T messageContent, SynchronizeHint hint);
}
