package us.ihmc.messager;

/**
 * This listener interface allows to pass an additional flag to indicate whether the listener should
 * operate asynchronous or synchronous.
 * 
 * @author Sylvain Bertrand
 * @param <T> the data type.
 */
public interface TopicListenerBase<T>
{
   /**
    * The messager just received data for the topic.
    * 
    * @param message the message.
    */
   void receivedMessageForTopic(Message<T> message);
}
