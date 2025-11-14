import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Test strict mode validation
 */
public class StrictModeTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Testing Strict Mode Validation ===\n");

            // Test 1: Array length mismatch should error in strict mode
            String test1 = "[3]: a,b"; // Declares 3, provides 2
            testStrictError(test1, "Array length mismatch");

            // Test 2: Invalid indentation (3 spaces, not multiple of 2)
            String test2 = "user:\n   id: 123"; // 3 spaces
            testStrictError(test2, "Invalid indentation (3 spaces)");

            // Test 3: Valid indentation should work
            String test3 = "user:\n  id: 123"; // 2 spaces
            testStrictSuccess(test3, "Valid indentation");

            // Test 4: Array with correct length
            String test4 = "[3]: a,b,c";
            testStrictSuccess(test4, "Array correct length");

            System.out.println("\n✅ All strict mode tests passed!");
        } catch (Exception e) {
            System.err.println("Test suite failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testStrictError(String input, String description) throws IOException {
        try {
            ToonParser parser = new ToonParser(new StringReader(input), 2, true); // strict=true

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Parse all events
            }

            // Should not reach here - should have errored
            System.out.println("✗ " + description + ": Expected error but succeeded");
            throw new AssertionError("Expected error for: " + description);
        } catch (IOException e) {
            // Expected error
            System.out.println("✓ " + description + ": Caught error as expected - " + e.getMessage());
        }
    }

    static void testStrictSuccess(String input, String description) throws IOException {
        try {
            ToonParser parser = new ToonParser(new StringReader(input), 2, true); // strict=true

            int eventCount = 0;
            ToonParser.Event event;
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 50) {
                // Parse all events
            }

            System.out.println("✓ " + description + ": Passed (" + eventCount + " events)");
        } catch (IOException e) {
            System.out.println("✗ " + description + ": Unexpected error - " + e.getMessage());
            throw e;
        }
    }
}
