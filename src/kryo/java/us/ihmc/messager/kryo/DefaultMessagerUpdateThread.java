package us.ihmc.messager.kryo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class DefaultMessagerUpdateThread implements MessagerUpdateThread
{
   private final ScheduledExecutorService executorService;
   private int periodMillis;
   private boolean running = false;

   public DefaultMessagerUpdateThread(String name, int periodMillis)
   {
      executorService = Executors.newSingleThreadScheduledExecutor(getNamedThreadFactory(name));
      this.periodMillis = periodMillis;
   }

   @Override
   public void start(Runnable runnable)
   {
      if (running)
      {
         throw new RuntimeException("Thread has already been scheduled");
      }

      executorService.scheduleAtFixedRate(runnable, 0, periodMillis, TimeUnit.MILLISECONDS);
      running = true;
   }

   @Override
   public void stop()
   {
      executorService.shutdown();
   }

   private ThreadFactory getNamedThreadFactory(final String name)
   {
      return new ThreadFactory()
      {
         private final AtomicInteger threadNumber = new AtomicInteger(1);

         public Thread newThread(Runnable r)
         {
            Thread t = new Thread(r, name + "-thread-" + threadNumber.getAndIncrement());

            if (t.isDaemon())
               t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
               t.setPriority(Thread.NORM_PRIORITY);

            return t;
         }
      };
   }
}
