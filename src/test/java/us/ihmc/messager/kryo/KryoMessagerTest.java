package us.ihmc.messager.kryo;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import us.ihmc.commons.MutationTestFacilitator;
import us.ihmc.log.LogTools;
import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.examples.EnglishPerson;
import us.ihmc.messager.examples.FrenchPerson;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static us.ihmc.messager.examples.EnglishPerson.*;
import static us.ihmc.messager.examples.FrenchPerson.*;

public class KryoMessagerTest
{

   private Messager clientMessager;
   private Messager serverMessager;

   @Test
   public void testKryoMessager()
   {
      Assertions.assertTimeoutPreemptively(Duration.ofSeconds(10), () -> runKryoMessagerTest());
   }

   private void runKryoMessagerTest() throws Exception
   {
      Map<String, String> englishToFrenchNumbers = new HashMap<>();
      Map<String, String> frenchToEnglishNumbers = new HashMap<>();
      englishToFrenchNumbers.put("one", "un");
      englishToFrenchNumbers.put("two", "deux");
      englishToFrenchNumbers.put("three", "trois");
      englishToFrenchNumbers.put("four", "quatre");
      englishToFrenchNumbers.put("five", "cinq");

      frenchToEnglishNumbers.put("un", "one");
      frenchToEnglishNumbers.put("deux", "two");
      frenchToEnglishNumbers.put("trois", "three");
      frenchToEnglishNumbers.put("quatre", "four");
      frenchToEnglishNumbers.put("cinq", "five");

      MutableObject<Runnable> serverUpdater = new MutableObject<>();
      MessagerUpdateThread serverManualCallUpdater = new MessagerUpdateThread()
      {
         @Override
         public void start(Runnable runnable)
         {
            serverUpdater.setValue(runnable);
         }

         @Override
         public void stop()
         {
            serverUpdater.setValue(null);
         }
      };

      MutableObject<Runnable> clientUpdater = new MutableObject<>();
      MessagerUpdateThread clientManualCallUpdater = new MessagerUpdateThread()
      {
         @Override
         public void start(Runnable runnable)
         {
            clientUpdater.setValue(runnable);
         }

         @Override
         public void stop()
         {
            clientUpdater.setValue(null);
         }
      };

      int tcpPort = 54557;

      Thread serverThread = new Thread(() -> {
         MessagerAPIFactory api = new MessagerAPIFactory();
         api.createRootCategory("TranslatorExample");
         api.includeMessagerAPIs(EnglishPerson.EnglishAPI, FrenchPerson.FrenchAPI);
         serverMessager = KryoMessager.createServer(api.getAPIAndCloseFactory(), tcpPort, serverManualCallUpdater);
         try
         {
            serverMessager.startMessager();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      });

      Thread clientThread = new Thread(() -> {
         MessagerAPIFactory api = new MessagerAPIFactory();
         api.createRootCategory("TranslatorExample");
         api.includeMessagerAPIs(EnglishPerson.EnglishAPI, FrenchPerson.FrenchAPI);
         clientMessager = KryoMessager.createClient(api.getAPIAndCloseFactory(), "localhost", tcpPort, clientManualCallUpdater);
         try
         {
            clientMessager.startMessager();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      });

      LogTools.info("Server connecting...");
      LogTools.info("Client connecting...");

      serverThread.start();
      clientThread.start();

      serverThread.join();
      clientThread.join();

      assertTrue(serverMessager.isMessagerOpen());
      assertTrue(clientMessager.isMessagerOpen());

      LogTools.info("Connected!");

      AtomicReference<String> englishInput = clientMessager.createInput(ListenEnglish, "I've heard nothing yet.");
      AtomicReference<String> frenchInput = serverMessager.createInput(ListenFrench, "Je n'ai encore rien entendu.");

      clientMessager.addTopicListener(ListenEnglish, message -> LogTools.info("Client: ListenEnglish: {}", message));
      clientMessager.addTopicListener(ListenFrench, message -> LogTools.info("Client: ListenFrench: {}", message));
      clientMessager.addTopicListener(SpeakEnglish, message -> LogTools.info("Client: SpeakEnglish: {}", message));
      clientMessager.addTopicListener(SpeakFrench, message -> LogTools.info("Client: SpeakFrench: {}", message));
      serverMessager.addTopicListener(ListenEnglish, message -> LogTools.info("Server: ListenEnglish: {}", message));
      serverMessager.addTopicListener(ListenFrench, message -> LogTools.info("Server: ListenFrench: {}", message));
      serverMessager.addTopicListener(SpeakEnglish, message -> LogTools.info("Server: SpeakEnglish: {}", message));
      serverMessager.addTopicListener(SpeakFrench, message -> LogTools.info("Server: SpeakFrench: {}", message));

      String[] numbers = {"un", "deux", "trois", "quatre", "cinq"};
      MutableInt count = new MutableInt();
      serverMessager.addTopicListener(ListenFrench, message -> frenchPersonListensToFrench(serverMessager, message, numbers, count));
      serverMessager.addTopicListener(SpeakEnglish, message -> bilingualPersonListensToEnglish(serverMessager, message, englishToFrenchNumbers));
      serverMessager.addTopicListener(SpeakFrench, message -> bilingualPersonListensToFrench(serverMessager, message, frenchToEnglishNumbers));

      LogTools.info("Latest french the english heard: {}", frenchInput.get());
      LogTools.info("Latest english the french heard: {}", englishInput.get());

      runUpdates(serverUpdater, clientUpdater);

      //      serverMessager.submitMessage(ListenEnglish, "Test message on ListenEnglish topic.");
      //      serverMessager.submitMessage(ListenFrench, "Test message on ListenEnglish topic.");
      runUpdates(serverUpdater, clientUpdater);

      serverMessager.submitMessage(SpeakEnglish, "Let the French person know I said five.");
      runUpdates(serverUpdater, clientUpdater);
      assertEquals("cinq", frenchInput.get(), "Should have heard 5");

      clientMessager.submitMessage(SpeakEnglish, "Let's tell the French one.");
      runUpdates(serverUpdater, clientUpdater);
      assertEquals("un", frenchInput.get(), "Should have heard 1");

      serverMessager.submitMessage(SpeakEnglish, "What about two.");
      runUpdates(serverUpdater, clientUpdater);
      assertEquals("deux", frenchInput.get(), "Should have heard 2");

      serverMessager.closeMessager();
   }

   private void runUpdates(MutableObject<Runnable> serverUpdater, MutableObject<Runnable> clientUpdater)
   {
//      ThreadTools.sleep(5);

//      for (int i = 0; i < 5; i++)
      {
         serverUpdater.getValue().run();
         clientUpdater.getValue().run();
      }
   }

   private void frenchPersonListensToFrench(Messager messager, String messageContent, String[] numbers, MutableInt count)
   {
      LogTools.info("Je viens tout juste d'entendre que {}", messageContent);
      messager.submitMessage(SpeakFrench, "Et bien moi je dis " + numbers[count.getAndIncrement()] + ".");
   }

   private void bilingualPersonListensToEnglish(Messager messager, String messageContent, Map<String, String> englishToFrenchNumbers)
   {
      LogTools.info("Bilingual person: I'm translating this to french: {}", messageContent);
      String frenchMessage = "Le bilingue ne sait pas traduire ce qu'a dit l'anglais.";
      for (Entry<String, String> entry : englishToFrenchNumbers.entrySet())
      {
         if (messageContent.toLowerCase().contains(entry.getKey()))
            frenchMessage = entry.getValue();
      }
      messager.submitMessage(ListenFrench, frenchMessage);
   }

   private void bilingualPersonListensToFrench(Messager messager, String messageContent, Map<String, String> frenchToEnglishNumbers)
   {
      LogTools.info("Personne bilingue: je traduis ceci en anglais: {}", messageContent);
      String englishMessage = "I have no clue what the French said.";

      for (Entry<String, String> entry : frenchToEnglishNumbers.entrySet())
      {
         if (messageContent.toLowerCase().contains(entry.getKey()))
            englishMessage = "the French said: " + entry.getValue();
      }
      messager.submitMessage(ListenEnglish, englishMessage);
   }

   public static void main(String[] args)
   {
      Class<?>[] applicationClasses = new Class[] {Messager.class, KryoMessager.class};
      Class<?>[] testClasses = new Class[] {KryoMessagerTest.class};
      MutationTestFacilitator.facilitateMutationTestForClasses(applicationClasses, testClasses);
   }
}
