package us.ihmc.messager.examples;

import static us.ihmc.messager.examples.EnglishPerson.*;
import static us.ihmc.messager.examples.FrenchPerson.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import us.ihmc.messager.Messager;
import us.ihmc.messager.TopicListener;

public class BilingualPerson
{
   private final Map<String, String> englishToFrenchNumbers = new HashMap<>();
   private final Map<String, String> frenchToEnglishNumbers = new HashMap<>();

   public BilingualPerson(Messager messager)
   {
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

      messager.registerTopicListener(SpeakEnglish, new TopicListener<String>()
      {
         @Override
         public void receivedMessageForTopic(String messageContent)
         {
            String frenchMessage = "Le bilingue ne sait pas traduire ce qu'a dit l'anglais.";
            for (Entry<String, String> entry : englishToFrenchNumbers.entrySet())
            {
               if (messageContent.toLowerCase().contains(entry.getKey()))
                  frenchMessage = "l'anglais a dit: " + entry.getValue();
            }
            messager.submitMessage(ListenFrench, frenchMessage);
         }
      });

      messager.registerTopicListener(SpeakFrench, new TopicListener<String>()
      {
         @Override
         public void receivedMessageForTopic(String messageContent)
         {
            String englishMessage = "I have no clue what the French said.";

            for (Entry<String, String> entry : frenchToEnglishNumbers.entrySet())
            {
               if (messageContent.toLowerCase().contains(entry.getKey()))
                  englishMessage = "the French said: " + entry.getValue();
            }
            messager.submitMessage(ListenEnglish, englishMessage);
         }
      });
   }
}
