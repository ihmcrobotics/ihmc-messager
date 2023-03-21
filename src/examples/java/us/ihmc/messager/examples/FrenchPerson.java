package us.ihmc.messager.examples;

import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.MessagerAPIFactory.Category;
import us.ihmc.messager.MessagerAPIFactory.CategoryTheme;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.messager.MessagerAPIFactory.TopicTheme;
import us.ihmc.messager.MessagerAPIFactory.TypedTopicTheme;
import us.ihmc.messager.TopicListener;

public class FrenchPerson
{
   private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();

   private static final Category Root = apiFactory.createRootCategory(apiFactory.createCategoryTheme("FrenchPerson"));

   private static final CategoryTheme French = apiFactory.createCategoryTheme("French");

   private static final TypedTopicTheme<String> Speak = apiFactory.createTypedTopicTheme("Speak");
   private static final TopicTheme Listen = apiFactory.createTopicTheme("Listen");

   public static final Topic<String> SpeakFrench = Root.child(French).topic(Speak);
   public static final Topic<String> ListenFrench = Root.child(French).topic(Listen);

   public static final MessagerAPI FrenchAPI = apiFactory.getAPIAndCloseFactory();

   private final AtomicReference<String> input;
   private final String prefix = getClass().getSimpleName() + " : ";

   public FrenchPerson(Messager messager, ScheduledExecutorService executorService)
   {
      input = messager.createInput(ListenFrench, "Je n'ai encore rien entendu.");
      System.out.println(prefix + "Initial input value: " + input.get());

      String[] numbers = {"un", "deux", "trois", "quatre", "cinq"};

      messager.addTopicListener(ListenFrench, new TopicListener<String>()
      {
         Random random = new Random(2342);

         @Override
         public void receivedMessageForTopic(String messageContent)
         {
            System.out.println(prefix + "Je viens tout juste d'entendre que " + messageContent);
            messager.submitMessage(SpeakFrench, "Et bien moi je dis " + numbers[random.nextInt(numbers.length)] + ".");
         }
      });
   }
}
