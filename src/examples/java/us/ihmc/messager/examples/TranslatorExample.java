package us.ihmc.messager.examples;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.SharedMemoryMessager;

public class TranslatorExample
{
   public static void main(String[] args) throws Exception
   {
      ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "Example"));

      MessagerAPIFactory api = new MessagerAPIFactory();
      api.createRootCategory("TranslatorExample");
      api.includeMessagerAPIs(EnglishPerson.EnglishAPI, FrenchPerson.FrenchAPI);
      Messager messager = new SharedMemoryMessager(api.getAPIAndCloseFactory());
      messager.startMessager();

      new EnglishPerson(messager, executorService);
      new FrenchPerson(messager, executorService);
      new BilingualPerson(messager);

      Thread.sleep(5000);
      messager.closeMessager();
      executorService.shutdown();
   }
}
