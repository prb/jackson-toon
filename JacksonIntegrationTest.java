import tools.jackson.dataformat.toon.*;
import com.fasterxml.jackson.core.*;
import java.io.*;

/**
 * Test full Jackson integration with TOON format
 */
public class JacksonIntegrationTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Jackson TOON Integration Tests ===\n");

            testFactoryParser();
            testFactoryGenerator();
            testRoundTripWithFactory();

            System.out.println("\n=== All Jackson integration tests passed! ===");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testFactoryParser() throws IOException {
        System.out.println("Test: ToonFactory Parser");
        String toon = "id: 123\nname: Alice\nactive: true";

        ToonFactory factory = new ToonFactory();
        JsonParser parser = factory.createParser(toon);

        System.out.println("Input TOON:");
        System.out.println(toon);
        System.out.println("\nParsed tokens:");

        JsonToken token;
        int count = 0;
        while ((token = parser.nextToken()) != null && count++ < 20) {
            System.out.print("  " + token);
            if (token == JsonToken.FIELD_NAME || token == JsonToken.VALUE_STRING) {
                System.out.print(" = " + parser.getText());
            } else if (token == JsonToken.VALUE_NUMBER_INT) {
                System.out.print(" = " + parser.getIntValue());
            }
            System.out.println();
        }

        parser.close();
        System.out.println("? ToonFactory parser works\n");
    }

    static void testFactoryGenerator() throws IOException {
        System.out.println("Test: ToonFactory Generator");

        StringWriter sw = new StringWriter();
        ToonFactory factory = new ToonFactory();
        JsonGenerator gen = factory.createGenerator(sw);

        gen.writeStartObject();
        gen.writeFieldName("userId");
        gen.writeNumber(456);
        gen.writeFieldName("userName");
        gen.writeString("Bob");
        gen.writeFieldName("tags");
        gen.writeStartArray();
        gen.writeString("admin");
        gen.writeString("developer");
        gen.writeEndArray();
        gen.writeEndObject();
        gen.close();

        String output = sw.toString();
        System.out.println("Generated TOON:");
        System.out.println(output);

        assertTrue(output.contains("userId: 456"), "Should contain userId");
        assertTrue(output.contains("userName: Bob"), "Should contain userName");
        assertTrue(output.contains("[2]:"), "Should have array");

        System.out.println("? ToonFactory generator works\n");
    }

    static void testRoundTripWithFactory() throws IOException {
        System.out.println("Test: Round Trip with ToonFactory");

        // Original data
        StringWriter sw1 = new StringWriter();
        ToonFactory factory = new ToonFactory();
        JsonGenerator gen = factory.createGenerator(sw1);

        gen.writeStartObject();
        gen.writeFieldName("product");
        gen.writeStartObject();
        gen.writeFieldName("id");
        gen.writeNumber(789);
        gen.writeFieldName("name");
        gen.writeString("Widget");
        gen.writeFieldName("price");
        gen.writeNumber(19.99);
        gen.writeEndObject();
        gen.writeEndObject();
        gen.close();

        String toon = sw1.toString();
        System.out.println("Generated TOON:");
        System.out.println(toon);

        // Parse it back
        JsonParser parser = factory.createParser(toon);
        StringWriter sw2 = new StringWriter();
        JsonGenerator gen2 = factory.createGenerator(sw2);

        JsonToken token;
        while ((token = parser.nextToken()) != null) {
            switch (token) {
                case START_OBJECT:
                    gen2.writeStartObject();
                    break;
                case END_OBJECT:
                    gen2.writeEndObject();
                    break;
                case START_ARRAY:
                    gen2.writeStartArray();
                    break;
                case END_ARRAY:
                    gen2.writeEndArray();
                    break;
                case FIELD_NAME:
                    gen2.writeFieldName(parser.getText());
                    break;
                case VALUE_STRING:
                    gen2.writeString(parser.getText());
                    break;
                case VALUE_NUMBER_INT:
                    gen2.writeNumber(parser.getIntValue());
                    break;
                case VALUE_NUMBER_FLOAT:
                    gen2.writeNumber(parser.getDoubleValue());
                    break;
                case VALUE_TRUE:
                    gen2.writeBoolean(true);
                    break;
                case VALUE_FALSE:
                    gen2.writeBoolean(false);
                    break;
                case VALUE_NULL:
                    gen2.writeNull();
                    break;
            }
        }

        parser.close();
        gen2.close();

        String toon2 = sw2.toString();
        System.out.println("\nRe-generated TOON:");
        System.out.println(toon2);

        // Both should be functionally equivalent
        assertTrue(toon2.contains("product:"), "Should preserve structure");
        assertTrue(toon2.contains("id: 789"), "Should preserve data");

        System.out.println("? Round trip with factory works\n");
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }
}
