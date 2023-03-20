package us.ihmc.messager;

/**
 * This listener interface allows to pass an additional flag to indicate whether the listener should
 * operate asynchronous or synchronous.
 * 
 * @author Sylvain Bertrand
 * @param <T> the data type.
 */
public interface TopicListenerSyncable<T> extends TopicListener<T>
{
   @Override
   default void receivedMessageForTopic(T messageContent)
   {
      receivedMessageForTopic(messageContent, SynchronizeHint.NONE);
   }

   /**
    * The messager just received data for the topic.
    * 
    * @param messageContent the data.
    * @param hint           hint for how the execution should be performed:
    *                       {@link SynchronizeHint#NONE} nothing is expected,
    *                       {@link SynchronizeHint#ASYNCHRONOUS} the listener should return as soon as
    *                       possible and the actual execution should be performed asynchronously,
    *                       {@link SynchronizeHint#SYNCHRONOUS} the listener should return only once
    *                       the execution is done.
    */
   void receivedMessageForTopic(T messageContent, SynchronizeHint hint);
}
