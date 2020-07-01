package annotations;

import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectToJsonConverterTest {

   @Test
   public void givenObjectNotSerializedThenExceptionThrown() throws JsonSerializationException {
      Object object = new Object();
      ObjectToJsonConverter serializer = new ObjectToJsonConverter();
      assertThrows(JsonSerializationException.class, () -> {
         serializer.convertToJson(object);
      });
   }

   @Test
   public void givenObjectSerializedThenTrueReturned() throws JsonSerializationException {
      Person person = new Person("soufiane", "cheouati", "34");
      ObjectToJsonConverter serializer = new ObjectToJsonConverter();
      String jsonString = serializer.convertToJson(person);
      assertEquals("{\"personAge\":\"34\",\"firstName\":\"Soufiane\",\"lastName\":\"Cheouati\"}", jsonString);
   }
}
