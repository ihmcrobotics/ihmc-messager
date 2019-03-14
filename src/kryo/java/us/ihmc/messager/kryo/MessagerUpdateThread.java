package us.ihmc.messager.kryo;

import us.ihmc.messager.Messager;

/**
 * Update thread interface designed for real networking based Messager implementations.
 */
public interface MessagerUpdateThread
{
   /**
    * Gives a handle to a runnable that should be called periodically to update the Messager.
    * Triggered by {@link Messager#startMessager()}
    *
    * @param runnable
    */
   void start(Runnable runnable);

   /**
    * A simple callback triggered by {@link Messager#closeMessager()}
    */
   void stop();
}
