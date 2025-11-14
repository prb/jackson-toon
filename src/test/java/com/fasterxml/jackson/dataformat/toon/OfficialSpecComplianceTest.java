package com.fasterxml.jackson.dataformat.toon;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test our implementation against official TOON spec test cases
 * from https://github.com/toon-format/spec/blob/main/tests/
 */
@DisplayName("Official TOON Spec Compliance Tests")
public class OfficialSpecComplianceTest {

    @Nested
    @DisplayName("Primitive Value Tests")
    class PrimitiveValueTests {

        @Test
        @DisplayName("Unquoted strings")
        void testUnquotedStrings() throws Exception {
            assertParsesTo("hello", "hello");
            assertParsesTo("Ada_99", "Ada_99");
        }

        @Test
        @DisplayName("Numbers")
        void testNumbers() throws Exception {
            assertParsesToInt("42", 42);
            assertParsesToDouble("3.14", 3.14);
            assertParsesToInt("-7", -7);
        }

        @Test
        @DisplayName("Booleans and null")
        void testBooleansAndNull() throws Exception {
            assertParsesToBoolean("true", true);
            assertParsesToBoolean("false", false);
            assertParsesToNull("null");
        }

        @Test
        @DisplayName("Quoted strings preserve literal values")
        void testQuotedStringsPreserveLiterals() throws Exception {
            assertParsesTo("\"true\"", "true");   // String, not boolean
            assertParsesTo("\"42\"", "42");       // String, not number
            assertParsesTo("\"null\"", "null");   // String, not null
        }

        @Test
        @DisplayName("Escape sequences")
        void testEscapeSequences() throws Exception {
            assertParsesTo("\"line1\\nline2\"", "line1\nline2");
            assertParsesTo("\"tab\\there\"", "tab\there");
            assertParsesTo("\"C:\\\\Users\\\\path\"", "C:\\Users\\path");
            assertParsesTo("\"say \\\"hello\\\"\"", "say \"hello\"");
        }
    }

    @Nested
    @DisplayName("Object Tests")
    class ObjectTests {

        @Test
        @DisplayName("Simple object")
        void testSimpleObject() throws Exception {
            String input = "id: 123\nname: Ada\nactive: true";
            assertParses(input);
        }

        @Test
        @DisplayName("Object with null")
        void testObjectWithNull() throws Exception {
            String input = "id: 123\nvalue: null";
            assertParses(input);
        }

        @Test
        @DisplayName("Quoted value with colon")
        void testQuotedValueWithColon() throws Exception {
            String input = "note: \"a:b\"";
            assertParses(input);
        }

        @Test
        @DisplayName("Quoted value with comma")
        void testQuotedValueWithComma() throws Exception {
            String input = "note: \"a,b\"";
            assertParses(input);
        }

        @Test
        @DisplayName("Value with spaces")
        void testValueWithSpaces() throws Exception {
            String input = "text: \" padded \"";
            assertParses(input);
        }
    }

    @Nested
    @DisplayName("Primitive Array Tests")
    class PrimitiveArrayTests {

        @Test
        @DisplayName("String array field")
        void testStringArrayField() throws Exception {
            String input = "tags:\n  [3]: reading,gaming,coding";
            assertParses(input);
        }

        @Test
        @DisplayName("Number array field")
        void testNumberArrayField() throws Exception {
            String input = "nums:\n  [3]: 1,2,3";
            assertParses(input);
        }

        @Test
        @DisplayName("Mixed primitive array")
        void testMixedPrimitiveArray() throws Exception {
            String input = "data:\n  [4]: x,y,true,10";
            assertParses(input);
        }

        @Test
        @DisplayName("Empty array")
        void testEmptyArray() throws Exception {
            String input = "items:\n  [0]:";
            assertParses(input);
        }

        @Test
        @DisplayName("Array with quoted strings")
        void testArrayWithQuotedStrings() throws Exception {
            String input = "items:\n  [3]: a,\"b,c\",\"d:e\"";
            assertParses(input);
        }
    }

    @Nested
    @DisplayName("Tabular Array Tests")
    class TabularArrayTests {

        @Test
        @DisplayName("Tabular array field")
        void testTabularArrayField() throws Exception {
            String input = "users:\n  [2]{id,name}:\n    1,Alice\n    2,Bob";
            assertParses(input);
        }
    }

    @Nested
    @DisplayName("Nested Object Tests")
    class NestedObjectTests {

        @Test
        @DisplayName("Empty nested object")
        void testEmptyNestedObject() throws Exception {
            String input = "user:";
            assertParses(input);
        }

        @Test
        @DisplayName("Simple nested object")
        void testSimpleNestedObject() throws Exception {
            String input = "user:\n  id: 123\n  name: Ada";
            assertParses(input);
        }

        @Test
        @DisplayName("Deeply nested object")
        void testDeeplyNestedObject() throws Exception {
            String input = "user:\n  address:\n    city: NYC\n    zip: 10001";
            assertParses(input);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Array with empty string")
        void testArrayWithEmptyString() throws Exception {
            String input = "items:\n  [1]: \"\"";
            assertParses(input);
        }

        @Test
        @DisplayName("Array with multiple empty strings")
        void testArrayWithMultipleEmptyStrings() throws Exception {
            String input = "items:\n  [3]: a,\"\",b";
            assertParses(input);
        }

        @Test
        @DisplayName("Array with whitespace strings")
        void testArrayWithWhitespaceStrings() throws Exception {
            String input = "items:\n  [2]: \" \",\"  \"";
            assertParses(input);
        }
    }

    // ========================================================================
    // Test Helpers
    // ========================================================================

    /**
     * Assert that input parses to expected string value
     */
    private void assertParsesTo(String input, String expected) throws Exception {
        ToonParser parser = new ToonParser(new StringReader(input));
        ToonParser.Event event = parser.nextEvent();

        assertEquals(ToonParser.Event.VALUE_STRING, event,
                "Expected VALUE_STRING event for input: " + escape(input));
        assertEquals(expected, parser.getTextValue(),
                "Value mismatch for input: " + escape(input));
    }

    /**
     * Assert that input parses to expected int value
     */
    private void assertParsesToInt(String input, int expected) throws Exception {
        ToonParser parser = new ToonParser(new StringReader(input));
        ToonParser.Event event = parser.nextEvent();

        assertEquals(ToonParser.Event.VALUE_NUMBER_INT, event,
                "Expected VALUE_NUMBER_INT event for input: " + escape(input));
        assertEquals(expected, parser.getNumberValue().intValue(),
                "Value mismatch for input: " + escape(input));
    }

    /**
     * Assert that input parses to expected double value
     */
    private void assertParsesToDouble(String input, double expected) throws Exception {
        ToonParser parser = new ToonParser(new StringReader(input));
        ToonParser.Event event = parser.nextEvent();

        assertEquals(ToonParser.Event.VALUE_NUMBER_FLOAT, event,
                "Expected VALUE_NUMBER_FLOAT event for input: " + escape(input));
        assertEquals(expected, parser.getNumberValue().doubleValue(), 0.0001,
                "Value mismatch for input: " + escape(input));
    }

    /**
     * Assert that input parses to expected boolean value
     */
    private void assertParsesToBoolean(String input, boolean expected) throws Exception {
        ToonParser parser = new ToonParser(new StringReader(input));
        ToonParser.Event event = parser.nextEvent();

        ToonParser.Event expectedEvent = expected ? ToonParser.Event.VALUE_TRUE : ToonParser.Event.VALUE_FALSE;
        assertEquals(expectedEvent, event,
                "Expected " + expectedEvent + " event for input: " + escape(input));
    }

    /**
     * Assert that input parses to null value
     */
    private void assertParsesToNull(String input) throws Exception {
        ToonParser parser = new ToonParser(new StringReader(input));
        ToonParser.Event event = parser.nextEvent();

        assertEquals(ToonParser.Event.VALUE_NULL, event,
                "Expected VALUE_NULL event for input: " + escape(input));
    }

    /**
     * Assert that input parses without errors (for complex structures)
     */
    private void assertParses(String input) throws Exception {
        ToonParser parser = new ToonParser(new StringReader(input));
        int eventCount = 0;
        ToonParser.Event event;

        // Verify it parses without errors
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF && eventCount++ < 100) {
            // Parse all events
        }

        assertTrue(eventCount < 100, "Too many events - possible infinite loop");
    }

    /**
     * Escape special characters for display
     */
    private String escape(String s) {
        return s.replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r");
    }
}
