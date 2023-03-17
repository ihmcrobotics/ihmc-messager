package us.ihmc.messager;

import java.util.Objects;

import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.messager.MessagerAPIFactory.TopicID;

/**
 * A message can be used with a {@link Messager}.
 * 
 * @author Sylvain Bertrand
 * @param <T> the type of data this message carries.
 */
public final class Message<T>
{
   /**
    * The ID of the topic the data is for.
    * <p>
    * This field is public and non-final only for serialization purposes, it is not meant to be
    * accessed directly.
    * </p>
    */
   public TopicID topicID;
   /**
    * The data this message carries.
    * <p>
    * This field is public and non-final only for serialization purposes, it is not meant to be
    * accessed directly.
    * </p>
    */
   public T messageContent;
   /**
    * For asynchronous implementations of the messager, this field provides a hint that the caller
    * wants the processing of the message to be synchronous.
    * <p>
    * When {@code true}, the caller expects that {@link Messager#submitMessage(Message)} is to return
    * only after the message has been processed by the active listeners.
    * </p>
    * <p>
    * Please verify with the messager implementation whether this option is supported.
    * </p>
    */
   public boolean synchronousHint = false;

   /** Empty constructor only used for serialization purposes. */
   public Message()
   {
   }

   /**
    * Creates a new message given a the data to carry for a given topic.
    * 
    * @param topic          the topic the data is for.
    * @param messageContent the data to carry.
    */
   public Message(Topic<T> topic, T messageContent)
   {
      this.topicID = topic.getUniqueID();
      this.messageContent = messageContent;
   }

   /**
    * Creates a new message given a the data to carry for a given topic.
    * 
    * @param topicID        the ID of the topic the data is for.
    * @param messageContent the data to carry.
    */
   public Message(TopicID topicID, T messageContent)
   {
      this.topicID = topicID;
      this.messageContent = messageContent;
   }

   /**
    * Creates a new message which specifies that it processing should be synchronous.
    * <p>
    * The caller expects that {@link Messager#submitMessage(Message)} is to return only after the
    * message has been processed by the active listeners.
    * </p>
    * 
    * @param topic          the topic the data is for.
    * @param messageContent the data to carry.
    * @return the new message
    */
   public static <T> Message<T> newSynchronizedMessage(TopicID topicID, T messageContent)
   {
      Message<T> message = new Message<>(topicID, messageContent);
      message.synchronousHint = true;
      return message;
   }

   /**
    * Copy constructor.
    * 
    * @param other the other message to copy.
    */
   public Message(Message<T> other)
   {
      set(other);
   }

   /**
    * Copy setter.
    * 
    * @param other the other message to copy.
    */
   public void set(Message<T> other)
   {
      topicID = other.topicID;
      messageContent = other.messageContent;
      synchronousHint = other.synchronousHint;
   }

   /**
    * Gets the ID of the topic this message is for.
    * 
    * @return the topic ID.
    */
   public TopicID getTopicID()
   {
      return topicID;
   }

   /**
    * Retrieves the topic from the given API this message is for.
    * 
    * @param api the API that contains the topic of this message.
    * @return the topic this message is for.
    */
   public Topic<T> getTopic(MessagerAPI api)
   {
      return api.findTopic(topicID);
   }

   /**
    * Gets the data this message is carrying.
    * 
    * @return the data this message is carrying.
    */
   public T getMessageContent()
   {
      return messageContent;
   }

   /**
    * For asynchronous implementations of the messager, this field provides a hint that the caller
    * wants the processing of the message to be synchronous.
    * <p>
    * When {@code true}, the caller expects that {@link Messager#submitMessage(Message)} is to return
    * only after the message has been processed by the active listeners.
    * </p>
    * <p>
    * Please verify with the messager implementation whether this option is supported.
    * </p>
    * 
    * @return {@code true} if synchronous message processing is desired, {@code false} if there are no
    *         expectations on how the message is to be processed.
    */
   public boolean isSynchronousHint()
   {
      return synchronousHint;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public boolean equals(Object object)
   {
      if (object == this)
         return true;
      else if (object instanceof Message other)
         return Objects.equals(topicID, other.topicID) && Objects.equals(messageContent, other.messageContent) && synchronousHint == other.synchronousHint;
      else
         return false;
   }
}
