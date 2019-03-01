package us.ihmc.messager;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.Test;
import us.ihmc.commons.MutationTestFacilitator;
import us.ihmc.log.LogTools;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.examples.EnglishPerson;
import us.ihmc.messager.examples.FrenchPerson;
import us.ihmc.messager.kryo.KryoMessager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static us.ihmc.messager.examples.EnglishPerson.ListenEnglish;
import static us.ihmc.messager.examples.EnglishPerson.SpeakEnglish;
import static us.ihmc.messager.examples.FrenchPerson.ListenFrench;
import static us.ihmc.messager.examples.FrenchPerson.SpeakFrench;

public class MessagerTest
{
   @Test
   public void testSharedMemoryMessager() throws Exception
   {
      testMessager(messagerAPI -> new SharedMemoryMessager(messagerAPI));
   }

   private void testMessager(MessagerSupplier messagerSupplier) throws Exception
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

      MessagerAPIFactory api = new MessagerAPIFactory();
      api.createRootCategory("TranslatorExample");
      api.includeMessagerAPIs(EnglishPerson.EnglishAPI, FrenchPerson.FrenchAPI);
      Messager messager = messagerSupplier.createMessager(api.getAPIAndCloseFactory());
      messager.startMessager();

      AtomicReference<String> englishInput = messager.createInput(ListenEnglish, "I've heard nothing yet.");
      AtomicReference<String> frenchInput = messager.createInput(ListenFrench, "Je n'ai encore rien entendu.");

      String[] numbers = {"un", "deux", "trois", "quatre", "cinq"};
      MutableInt count = new MutableInt();
      messager.registerTopicListener(ListenFrench, message -> frenchPersonListensToFrench(messager, message, numbers, count));
      messager.registerTopicListener(SpeakEnglish, message -> bilingualPersonListensToEnglish(messager, message, englishToFrenchNumbers));
      messager.registerTopicListener(SpeakFrench, message -> bilingualPersonListensToFrench(messager, message, frenchToEnglishNumbers));

      LogTools.info("Latest french the english heard: {}", frenchInput.get());
      LogTools.info("Latest english the french heard: {}", englishInput.get());

      messager.submitMessage(SpeakEnglish, "Let the French person know I said five.");
      assertEquals("cinq", frenchInput.get(), "Should have heard 5");
      messager.submitMessage(SpeakEnglish, "Let's tell the French one.");
      assertEquals("un", frenchInput.get(), "Should have heard 1");
      messager.submitMessage(SpeakEnglish, "What about two.");
      assertEquals("deux", frenchInput.get(), "Should have heard 2");

      messager.closeMessager();
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

   interface MessagerSupplier
   {
      Messager createMessager(MessagerAPI messagerAPI);
   }

   public static void main(String[] args)
   {
      Class<?>[] applicationClasses = new Class[] {Messager.class, SharedMemoryMessager.class};
      Class<?>[] testClasses = new Class[] {MessagerTest.class};
      MutationTestFacilitator.facilitateMutationTestForClasses(applicationClasses, testClasses);
   }
}
