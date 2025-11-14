import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Test delimiter support
 */
public class DelimiterTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Delimiter Support ===\n");

            // Test 1: Default comma delimiter
            String test1 = "[3]: a,b,c";
            testParse(test1, "Default comma delimiter");

            // Test 2: Tabular array with comma (default)
            String test2 = "[2]{id,name}:\n  1,Alice\n  2,Bob";
            testParse(test2, "Tabular array comma delimiter");

            // Test 3: Quoted strings containing commas
            String test3 = "[3]: \"a,x\",\"b,y\",\"c,z\"";
            testParse(test3, "Inline array with quoted comma values");

            // Test 4: Mixed primitives
            String test4 = "[4]: 1,true,null,text";
            testParse(test4, "Mixed primitive array");

            System.out.println("\n✅ All delimiter tests passed!");
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
