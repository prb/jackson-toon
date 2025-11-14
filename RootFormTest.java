import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Test root form detection (single primitive/array at root)
 */
public class RootFormTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Root Form Detection ===\n");

            // Test 1: Single string at root
            String test1 = "hello";
            testParse(test1, "Root string", ToonParser.Event.VALUE_STRING);

            // Test 2: Single number at root
            String test2 = "42";
            testParse(test2, "Root number", ToonParser.Event.VALUE_NUMBER_INT);

            // Test 3: Single boolean at root
            String test3 = "true";
            testParse(test3, "Root boolean", ToonParser.Event.VALUE_TRUE);

            // Test 4: Single null at root
            String test4 = "null";
            testParse(test4, "Root null", ToonParser.Event.VALUE_NULL);

            // Test 5: Array at root
            String test5 = "[3]: a,b,c";
            testParse(test5, "Root array", ToonParser.Event.START_ARRAY);

            System.out.println("\n✅ All root form tests passed!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testParse(String input, String description, ToonParser.Event expectedFirst) throws IOException {
        try {
            ToonParser parser = new ToonParser(new StringReader(input));

            ToonParser.Event event = parser.nextEvent();
            if (event != expectedFirst) {
                throw new AssertionError("Expected " + expectedFirst + " but got " + event);
            }

            System.out.println("✓ " + description + ": " + event);
        } catch (Exception e) {
            System.out.println("✗ " + description + ": " + e.getMessage());
            throw e;
        }
    }
}
