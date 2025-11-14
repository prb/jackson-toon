import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Test blank line tolerance
 */
public class BlankLineTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Blank Line Tolerance ===\n");

            // Test 1: Blank lines in objects
            String test1 = "id: 123\n\nname: Ada\n\nactive: true";
            testParse(test1, "Blank lines in object");

            // Test 2: Blank lines in list arrays
            String test2 = "[3]:\n  - item1\n\n  - item2\n\n  - item3";
            testParse(test2, "Blank lines in list array");

            // Test 3: Multiple consecutive blank lines
            String test3 = "user:\n\n\n  id: 123";
            testParse(test3, "Multiple blank lines");

            // Test 4: Blank line before closing
            String test4 = "user:\n  id: 123\n\n";
            testParse(test4, "Blank line before end");

            System.out.println("\n✅ All blank line tests passed!");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testParse(String input, String description) throws IOException {
        try {
            ToonParser parser = new ToonParser(new StringReader(input));

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Just parse all events
            }

            System.out.println("✓ " + description + " (" + eventCount + " events)");
        } catch (Exception e) {
            System.out.println("✗ " + description + ": " + e.getMessage());
            throw e;
        }
    }
}
