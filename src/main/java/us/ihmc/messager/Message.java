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
    * Hint for how the execution inside the listener should be performed:
    * <ul>
    * <li>{@link SynchronizeHint#NONE} nothing is expected,
    * <li>{@link SynchronizeHint#ASYNCHRONOUS} the listener should return as soon as possible and the
    * actual execution should be performed asynchronously,
    * <li>{@link SynchronizeHint#SYNCHRONOUS} the listener should return only once the execution is
    * done.
    * </ul>
    */
   public SynchronizeHint synchronizeHint;

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
      synchronizeHint = other.synchronizeHint;
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
    * Sets the hint for how the execution inside the listener should be performed:
    * <ul>
    * <li>{@link SynchronizeHint#NONE} nothing is expected,
    * <li>{@link SynchronizeHint#ASYNCHRONOUS} the listener should return as soon as possible and the
    * actual execution should be performed asynchronously,
    * <li>{@link SynchronizeHint#SYNCHRONOUS} the listener should return only once the execution is
    * done.
    * </ul>
    * 
    * @param synchronizeHint the new hint value.
    */
   public void setSynchronizeHint(SynchronizeHint synchronizeHint)
   {
      this.synchronizeHint = synchronizeHint;
   }

   /**
    * Returns the hint for how the execution inside the listener should be performed:
    * <ul>
    * <li>{@link SynchronizeHint#NONE} nothing is expected,
    * <li>{@link SynchronizeHint#ASYNCHRONOUS} the listener should return as soon as possible and the
    * actual execution should be performed asynchronously,
    * <li>{@link SynchronizeHint#SYNCHRONOUS} the listener should return only once the execution is
    * done.
    * </ul>
    * 
    * @return the hint value.
    */
   public SynchronizeHint getSynchronizeHint()
   {
      return synchronizeHint;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public boolean equals(Object object)
   {
      if (object == this)
      {
         return true;
      }
      else if (object instanceof Message other)
      {
         if (!Objects.equals(topicID, other.topicID))
            return false;
         if (!Objects.equals(messageContent, other.messageContent))
            return false;
         if (!Objects.equals(synchronizeHint, other.synchronizeHint))
            return false;
         return true;
      }
      else
      {
         return false;
      }
   }
}
