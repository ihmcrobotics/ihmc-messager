package us.ihmc.messager;

/**
 * Enum used to provide a hint on how processing of a new message in a listener should be executed.
 * The hint can be provided when submitting a message via
 * {@link Messager#submitMessage(MessagerAPIFactory.Topic, Object, SynchronizeHint)}. The hint
 * should be applied on a best effort basis.
 */
public enum SynchronizeHint
{
   /**
    * The caller expects the listener to return as soon as possible and perform the actual execution
    * asynchronously.
    */
   ASYNCHRONOUS,
   /** The caller expect the listener to return only once the execution is done. */
   SYNCHRONOUS,
   /** No expectation for how the listener perform the execution. */
   NONE
}