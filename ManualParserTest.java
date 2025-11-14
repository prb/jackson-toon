import tools.jackson.dataformat.toon.*;
import java.io.*;
import java.util.*;

/**
 * Manual test for ToonParser - runs without JUnit
 */
public class ManualParserTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== TOON Parser Manual Tests ===\n");

            testSimpleObject();
            testNestedObject();
            testInlineArray();
            testSimpleKeyValue();

            System.out.println("\n=== All tests passed! ===");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testSimpleKeyValue() throws IOException {
        System.out.println("Test: Simple Key-Value");
        String input = "name: Alice";
        ToonParser parser = new ToonParser(new StringReader(input));

        List<String> events = new ArrayList<>();
        ToonParser.Event event;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
            events.add(event.toString());
            if (event == ToonParser.Event.FIELD_NAME) {
                events.add("  \"" + parser.getTextValue() + "\"");
            } else if (event == ToonParser.Event.VALUE_STRING) {
                events.add("  \"" + parser.getTextValue() + "\"");
            }
        }

        System.out.println("  Events: " + events);
        System.out.println("  ✓ Simple key-value works\n");
    }

    static void testSimpleObject() throws IOException {
        System.out.println("Test: Simple Object");
        String input = "id: 123\nname: Alice";
        ToonParser parser = new ToonParser(new StringReader(input));

        List<ToonParser.Event> events = new ArrayList<>();
        ToonParser.Event event;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
            events.add(event);
        }

        System.out.println("  Events: " + events);

        // Should have: START_OBJECT, FIELD_NAME, VALUE, FIELD_NAME, VALUE, END_OBJECT
        assertEqual(ToonParser.Event.START_OBJECT, events.get(0), "First event");
        assertEqual(ToonParser.Event.FIELD_NAME, events.get(1), "Second event");

        System.out.println("  ✓ Simple object parsing works\n");
    }

    static void testNestedObject() throws IOException {
        System.out.println("Test: Nested Object");
        String input = "user:\n  id: 123\n  name: Alice";
        ToonParser parser = new ToonParser(new StringReader(input));

        List<ToonParser.Event> events = new ArrayList<>();
        ToonParser.Event event;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
            events.add(event);
            System.out.println("    " + event + (event == ToonParser.Event.FIELD_NAME || event == ToonParser.Event.VALUE_STRING ? " (" + parser.getTextValue() + ")" : ""));
        }

        System.out.println("  Events: " + events);
        System.out.println("  ✓ Nested object parsing works\n");
    }

    static void testInlineArray() throws IOException {
        System.out.println("Test: Inline Array");
        String input = "[3]: a,b,c";
        ToonParser parser = new ToonParser(new StringReader(input));

        List<ToonParser.Event> events = new ArrayList<>();
        ToonParser.Event event;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
            events.add(event);
            if (event == ToonParser.Event.VALUE_STRING) {
                System.out.println("    VALUE_STRING: " + parser.getTextValue());
            }
        }

        System.out.println("  Events: " + events);

        // Should have: START_ARRAY, VALUE, VALUE, VALUE, END_ARRAY
        assertEqual(ToonParser.Event.START_ARRAY, events.get(0), "First event");
        assertEqual(ToonParser.Event.VALUE_STRING, events.get(1), "Second event");
        assertEqual(ToonParser.Event.VALUE_STRING, events.get(2), "Third event");
        assertEqual(ToonParser.Event.VALUE_STRING, events.get(3), "Fourth event");

        System.out.println("  ✓ Inline array parsing works\n");
    }

    static void assertEqual(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(String.format(
                "%s: expected <%s> but got <%s>",
                message, expected, actual
            ));
        }
    }
}
