package us.ihmc.messager.examples;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import us.ihmc.messager.Messager;
import us.ihmc.messager.MessagerAPIFactory;
import us.ihmc.messager.MessagerAPIFactory.Category;
import us.ihmc.messager.MessagerAPIFactory.CategoryTheme;
import us.ihmc.messager.MessagerAPIFactory.MessagerAPI;
import us.ihmc.messager.MessagerAPIFactory.Topic;
import us.ihmc.messager.MessagerAPIFactory.TopicTheme;
import us.ihmc.messager.MessagerAPIFactory.TypedTopicTheme;

public class EnglishPerson
{
   private static final MessagerAPIFactory apiFactory = new MessagerAPIFactory();

   private static final Category Root = apiFactory.createRootCategory(apiFactory.createCategoryTheme("EnglishPerson"));

   private static final CategoryTheme English = apiFactory.createCategoryTheme("English");

   private static final TypedTopicTheme<String> Speak = apiFactory.createTypedTopicTheme("Speak");
   private static final TopicTheme Listen = apiFactory.createTopicTheme("Listen");

   public static final Topic<String> SpeakEnglish = Root.child(English).topic(Speak);
   public static final Topic<String> ListenEnglish = Root.child(English).topic(Listen);

   public static final MessagerAPI EnglishAPI = apiFactory.getAPIAndCloseFactory();

   private final AtomicReference<String> input;
   private final String prefix = getClass().getSimpleName() + ": ";

   public EnglishPerson(Messager messager, ScheduledExecutorService executorService)
   {
      input = messager.createInput(ListenEnglish, "I've heard nothing yet.");

      executorService.scheduleAtFixedRate(() -> System.out.println(prefix + "Last time I hear from him, " + input.get()), 0, 500, TimeUnit.MILLISECONDS);

      executorService.schedule(() -> messager.submitMessage(SpeakEnglish, "Let me the French person know I said five."), 1, TimeUnit.SECONDS);

      executorService.schedule(() -> messager.submitMessage(SpeakEnglish, "Let's tell the French one."), 2, TimeUnit.SECONDS);

      executorService.schedule(() -> messager.submitMessage(SpeakEnglish, "What about two."), 2500, TimeUnit.MILLISECONDS);

   }
}
