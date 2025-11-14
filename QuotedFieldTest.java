import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Test if quoted field names work
 */
public class QuotedFieldTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Quoted Field Names ===\n");

            // Test 1: Quoted field with colon
            testParse("\"order:id\": 7", "Field with colon");

            // Test 2: Quoted field with brackets
            testParse("\"[index]\": 5", "Field with brackets");

            // Test 3: Quoted field with spaces
            testParse("\"full name\": Ada", "Field with spaces");

            // Test 4: Quoted field with comma
            testParse("\"a,b\": 1", "Field with comma");

            // Test 5: Quoted empty string as field
            testParse("\"\": 1", "Empty string field");

            // Test 6: Quoted numeric field
            testParse("\"123\": x", "Numeric field");

            System.out.println("\n✅ All quoted field tests passed!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testParse(String input, String description) throws IOException {
        try {
            ToonParser parser = new ToonParser(new StringReader(input));

            ToonParser.Event event1 = parser.nextEvent();
            if (event1 != ToonParser.Event.START_OBJECT) {
                throw new AssertionError("Expected START_OBJECT, got " + event1);
            }

            ToonParser.Event event2 = parser.nextEvent();
            if (event2 != ToonParser.Event.FIELD_NAME) {
                throw new AssertionError("Expected FIELD_NAME, got " + event2);
            }

            String fieldName = parser.getTextValue();
            System.out.println("✓ " + description + ": field=\"" + fieldName + "\"");
        } catch (Exception e) {
            System.out.println("✗ " + description + ": " + e.getMessage());
            throw e;
        }
    }
}
