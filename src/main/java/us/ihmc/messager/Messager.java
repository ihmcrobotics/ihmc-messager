package us.ihmc.messager;

/**
 * Implements this interface to create a simple messager that can either transport messages using
 * some shared memory or over network.
 * 
 * @author Sylvain Bertrand
 */
public interface Messager extends MessagerBasics
{
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
    * Notifies all the messager state listeners of the current state of this messager.
    */
   void notifyMessagerStateListeners();
}