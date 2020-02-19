package us.ihmc.messager.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This allows Kryo to send unmodifiable lists which do not have an empty constructor.
 */
public class UnmodifiableListSerializer extends CollectionSerializer
{
   @Override
   protected Collection create(Kryo kryo, Input input, Class<Collection> type)
   {
      return new ArrayList();
   }

   @Override
   protected Collection createCopy(Kryo kryo, Collection original)
   {
      return new ArrayList();
   }

   @Override
   public Collection read(Kryo kryo, Input input, Class<Collection> type)
   {
      List read = (List) super.read(kryo, input, type);
      return Collections.unmodifiableList(read);
   }

   @Override
   public Collection copy(Kryo kryo, Collection original)
   {
      List copy = (List) super.copy(kryo, original);
      return Collections.unmodifiableList(copy);
   }
}
