package us.ihmc.messager.javafx;

import us.ihmc.messager.Message;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory.TopicID;

/**
 * Wrapper class for adding a synchronous hint to a message that can be used to indicate whether the
 * caller expect the message submission to terminate after all active listeners have processed the
 * message.
 * 
 * @author Sylvain Bertrand
 * @param <T>
 */
public class SyncMessageContentWrapper<T>
{
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

   public SyncMessageContentWrapper(T messageContent, boolean synchronousHint)
   {
      this.messageContent = messageContent;
      this.synchronousHint = synchronousHint;
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
   public static <T> Message<SyncMessageContentWrapper<T>> newSynchronizedMessage(TopicID topicID, T messageContent)
   {
      return new Message<>(topicID, new SyncMessageContentWrapper<T>(messageContent, true));
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
}
