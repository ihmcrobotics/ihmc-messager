package us.ihmc.messager.kryo;

public interface MessagerUpdateThread
{
   void start(Runnable runnable);

   void stop();
}
