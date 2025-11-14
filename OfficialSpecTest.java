import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Test our implementation against official TOON spec test cases
 * from https://github.com/toon-format/spec/blob/main/tests/
 */
public class OfficialSpecTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        try {
            System.out.println("=== Official TOON Spec Compliance Tests ===\n");

            testPrimitives();
            testObjects();
            testArraysPrimitive();
            testArraysTabular();
            testNestedObjects();
            testEdgeCases();

            System.out.println("\n=== Test Summary ===");
            System.out.println("Passed: " + passed);
            System.out.println("Failed: " + failed);

            if (failed > 0) {
                System.out.println("\n⚠ Some tests failed - implementation incomplete");
                System.exit(1);
            } else {
                System.out.println("\n✓ All tested cases passed!");
            }
        } catch (Exception e) {
            System.err.println("Test suite failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testPrimitives() {
        System.out.println("=== Primitive Value Tests ===");

        // Test 1: Unquoted strings
        testParse("hello", "hello");
        testParse("Ada_99", "Ada_99");

        // Test 2: Numbers
        testParse("42", 42);
        testParse("3.14", 3.14);
        testParse("-7", -7);

        // Test 3: Booleans and null
        testParse("true", true);
        testParse("false", false);
        testParse("null", null);

        // Test 4: Quoted strings preserve literal values
        testParse("\"true\"", "true");  // String, not boolean
        testParse("\"42\"", "42");      // String, not number
        testParse("\"null\"", "null");  // String, not null

        // Test 5: Escape sequences
        testParse("\"line1\\nline2\"", "line1\nline2");
        testParse("\"tab\\there\"", "tab\there");
        testParse("\"C:\\\\Users\\\\path\"", "C:\\Users\\path");
        testParse("\"say \\\"hello\\\"\"", "say \"hello\"");

        System.out.println();
    }

    static void testObjects() {
        System.out.println("=== Object Tests ===");

        // Test 1: Simple object
        String input1 = "id: 123\nname: Ada\nactive: true";
        testParseObject(input1, "Simple object");

        // Test 2: Object with null
        String input2 = "id: 123\nvalue: null";
        testParseObject(input2, "Object with null");

        // Test 3: Quoted values with special chars
        String input3 = "note: \"a:b\"";
        testParseObject(input3, "Quoted value with colon");

        String input4 = "note: \"a,b\"";
        testParseObject(input4, "Quoted value with comma");

        // Test 4: Leading/trailing spaces
        String input5 = "text: \" padded \"";
        testParseObject(input5, "Value with spaces");

        System.out.println();
    }

    static void testArraysPrimitive() {
        System.out.println("=== Primitive Array Tests ===");

        // Test 1: String array as field value
        String input1 = "tags:\n  [3]: reading,gaming,coding";
        testParseObject(input1, "String array field");

        // Test 2: Number array as field value
        String input2 = "nums:\n  [3]: 1,2,3";
        testParseObject(input2, "Number array field");

        // Test 3: Mixed array
        String input3 = "data:\n  [4]: x,y,true,10";
        testParseObject(input3, "Mixed primitive array");

        // Test 4: Empty array
        String input4 = "items:\n  [0]:";
        testParseObject(input4, "Empty array");

        // Test 5: Array with quoted strings
        String input5 = "items:\n  [3]: a,\"b,c\",\"d:e\"";
        testParseObject(input5, "Array with quoted strings");

        System.out.println();
    }

    static void testArraysTabular() {
        System.out.println("=== Tabular Array Tests ===");

        // Test 1: Tabular array as field value
        String input1 = "users:\n  [2]{id,name}:\n    1,Alice\n    2,Bob";
        testParseObject(input1, "Tabular array field");

        System.out.println();
    }

    static void testNestedObjects() {
        System.out.println("=== Nested Object Tests ===");

        // Test 1: Empty nested object
        String input1 = "user:";
        testParseObject(input1, "Empty nested object");

        // Test 2: Simple nested object
        String input2 = "user:\n  id: 123\n  name: Ada";
        testParseObject(input2, "Simple nested object");

        // Test 3: Deeply nested
        String input3 = "user:\n  address:\n    city: NYC\n    zip: 10001";
        testParseObject(input3, "Deeply nested object");

        System.out.println();
    }

    static void testEdgeCases() {
        System.out.println("=== Edge Case Tests ===");

        // Test 1: Empty string in array
        String input1 = "items:\n  [1]: \"\"";
        testParseObject(input1, "Array with empty string");

        // Test 2: Multiple empty strings
        String input2 = "items:\n  [3]: a,\"\",b";
        testParseObject(input2, "Array with multiple empty strings");

        // Test 3: Whitespace-only strings
        String input3 = "items:\n  [2]: \" \",\"  \"";
        testParseObject(input3, "Array with whitespace strings");

        System.out.println();
    }

    // ========================================================================
    // Test Helpers
    // ========================================================================

    static void testParse(String input, Object expected) {
        try {
            ToonParser parser = new ToonParser(new StringReader(input));
            ToonParser.Event event = parser.nextEvent();

            Object actual = null;
            if (event == ToonParser.Event.VALUE_STRING) {
                actual = parser.getTextValue();
            } else if (event == ToonParser.Event.VALUE_NUMBER_INT) {
                actual = parser.getNumberValue().intValue();
            } else if (event == ToonParser.Event.VALUE_NUMBER_FLOAT) {
                actual = parser.getNumberValue().doubleValue();
            } else if (event == ToonParser.Event.VALUE_TRUE) {
                actual = true;
            } else if (event == ToonParser.Event.VALUE_FALSE) {
                actual = false;
            } else if (event == ToonParser.Event.VALUE_NULL) {
                actual = null;
            }

            if (expected == null ? actual == null : expected.equals(actual)) {
                System.out.println("  ✓ " + escape(input) + " → " + formatValue(expected));
                passed++;
            } else {
                System.out.println("  ✗ " + escape(input) + " → expected " + formatValue(expected) + " but got " + formatValue(actual));
                failed++;
            }
        } catch (Exception e) {
            System.out.println("  ✗ " + escape(input) + " → ERROR: " + e.getMessage());
            failed++;
        }
    }

    static void testParseObject(String input, String description) {
        try {
            ToonParser parser = new ToonParser(new StringReader(input));
            int eventCount = 0;
            ToonParser.Event event;

            // Just verify it parses without errors
            while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 100) {
                // Parse all events
            }

            System.out.println("  ✓ " + description);
            passed++;
        } catch (Exception e) {
            System.out.println("  ✗ " + description + " → ERROR: " + e.getMessage());
            failed++;
        }
    }

    static String escape(String s) {
        return s.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
    }

    static String formatValue(Object v) {
        if (v == null) return "null";
        if (v instanceof String) return "\"" + v + "\"";
        return v.toString();
    }
}
