package us.ihmc.messager;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements this interface to create a simple messager that can either transport messages using
 * some shared memory or over network.
 * <p>
 * It could be argued by some that "Messenger" should be used as this class name, especially in the
 * hope of having this framework used by civilized people. However, the use of "Messager" stands
 * strong especially when considering the foundations of developer-friendly software library. It
 * indeed facilitates the search for the different classes in this project by simply searching for
 * "message" (messenger would not be found in such case). Also note while not popular, the word
 * "messager" is no foreigner to the American English as supported by our dear friend
 * <a href="https://www.websters1913.com/words/Messager">Webster's dictionary</a>. Finally note that
 * this sweet sounding "messager" comes from the froo-froo fancy French language (I can already
 * imagine you are being filled with the desire to open your favorite Internet browser to initiate a
 * quest hopefully leading you to listening to the French pronunciation.... here you go =>
 * <a href="https://en.wiktionary.org/wiki/messager">Wikitionary</a>), who would think that a little
 * sprinkle of a "French touch" there and there in our everyday software should be something to be
 * frowned upon.
 * </p>
 *
 * @author Sylvain Bertrand
 */
public interface Messager
{
   /**
    * Sends data for a given topic.
    *
    * @param topic          the topic of the data.
    * @param messageContent the data.
    */
   default <T> void submitMessage(MessagerAPIFactory.Topic<T> topic, T messageContent)
   {
      submitMessage(new Message<>(topic, messageContent));
   }

   /**
    * Sends a message.
    *
    * @param message the message to send.
    */
   <T> void submitMessage(Message<T> message);

   /**
    * Creates a variable which is to be automatically updated when this messager receives data destined
    * to the given topic.
    *
    * @param topic        the topic to listen to.
    * @param initialValue the initial value of the newly created variable.
    * @return a variable that is updated automatically when receiving new data.
    */
   <T> AtomicReference<T> createInput(MessagerAPIFactory.Topic<T> topic, T initialValue);

   /**
    * Attaches an existing AtomicReference as an input which is to be automatically updated when this
    * messager receives data destined to the given topic.
    *
    * @param topic the topic to listen to.
    * @param input an existing AtomicReference.
    */
   <T> void attachInput(MessagerAPIFactory.Topic<T> topic, AtomicReference<T> input);

   /**
    * Creates a variable which is to be automatically updated when this messager receives data destined
    * to the given topic.
    *
    * @param topic the topic to listen to.
    * @return a variable that is updated automatically when receiving new data.
    */
   default <T> AtomicReference<T> createInput(MessagerAPIFactory.Topic<T> topic)
   {
      return createInput(topic, null);
   }

   /**
    * Removes an input that was previously created by this messager.
    *
    * @param topic the topic the input is listening to.
    * @param input the input to be removed from this messager.
    * @return {@code true} if the internal list of inputs was modified by this operation, {@code false}
    *         otherwise.
    */
   <T> boolean removeInput(MessagerAPIFactory.Topic<T> topic, AtomicReference<T> input);

   /**
    * Registers a listener to be notified when new data is received for the given topic.
    *
    * @param topic    the topic to listen to.
    * @param listener the listener to be registered.
    */
   <T> void registerTopicListener(MessagerAPIFactory.Topic<T> topic, TopicListener<T> listener);

   /**
    * Removes a listener that was previously registered to this messager.
    *
    * @param topic    the topic the listener is listening to.
    * @param listener the listener to be removed.
    * @return {@code true} if the internal list of inputs was modified by this operation, {@code false}
    *         otherwise.
    */
   <T> boolean removeTopicListener(MessagerAPIFactory.Topic<T> topic, TopicListener<T> listener);

   /**
    * Opens this messager to start sending and receiving messages.
    *
    * @throws Exception depends on the implementation of messager.
    */
   void startMessager() throws Exception;

   /**
    * Closes this messager, no message can be sent once a messager is closed.
    *
    * @throws Exception depends on the implementation of messager.
    */
   void closeMessager() throws Exception;

   /**
    * Tests whether this messager is currently open, i.e. whether messages can be sent and received.
    *
    * @return {@code true} if this messager is open, {@code false} if it is closed.
    */
   boolean isMessagerOpen();

   /**
    * Notifies all the messager state listeners of the current state of this messager.
    */
   void notifyMessagerStateListeners();

   /**
    * Registers a new listener that is to be notified when the state of this messager changes.
    *
    * @param listener the listener to register.
    */
   void registerMessagerStateListener(MessagerStateListener listener);

   /**
    * Removes a listener previously registered to this messager.
    *
    * @param listener the listener to remove.
    * @return {@code true} if the internal list of inputs was modified by this operation, {@code false}
    *         otherwise.
    */
   boolean removeMessagerStateListener(MessagerStateListener listener);

   /**
    * Gets the API used by this messager.
    *
    * @return this messger's API.
    */
   MessagerAPIFactory.MessagerAPI getMessagerAPI();

   /**
    * Indicates whether this implementation supports synchronous messages.
    * 
    * @return is the synchronous option is supported. Default is {@code false}.
    * @see Message#synchronousHint
    */
   default boolean isSynchronousSupported()
   {
      return false;
   }
}