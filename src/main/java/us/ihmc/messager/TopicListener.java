package us.ihmc.messager;

/**
 * Implement this interface to create a listener to be notified when a messager receives data for a
 * given topic.
 * 
 * @author Sylvain Bertrand
 * @param <T> the data type.
 */
public interface TopicListener<T> extends TopicListenerBase<T>
{
   @Override
   default void receivedMessageForTopic(Message<T> message)
   {
      receivedMessageForTopic(message.getMessageContent());
   }

   /**
    * The messager just received data for the topic.
    * 
    * @param messageContent the data.
    */
   public void receivedMessageForTopic(T messageContent);
}
