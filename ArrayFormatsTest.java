import tools.jackson.dataformat.toon.*;
import java.io.*;
import java.util.*;

/**
 * Test all TOON array formats
 */
public class ArrayFormatsTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== TOON Array Formats Test ===\n");

            testInlineArraySameLine();
            testInlineArrayMultiLine();
            testTabularArray();
            testListArray();

            System.out.println("\n=== All array tests passed! ===");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testInlineArraySameLine() throws IOException {
        System.out.println("Test: Inline Array (same line)");
        String input = "[3]: a,b,c";
        ToonParser parser = new ToonParser(new StringReader(input));

        List<String> events = new ArrayList<>();
        ToonParser.Event event;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
            events.add(event.toString());
        }

        System.out.println("  Input: " + input);
        System.out.println("  Events: " + events);
        assertEqual(5, events.size(), "Event count");
        assertEqual("START_ARRAY", events.get(0), "First event");
        assertEqual("VALUE_STRING", events.get(1), "Second event");
        assertEqual("END_ARRAY", events.get(4), "Last event");
        System.out.println("  ✓ Same-line inline array works\n");
    }

    static void testInlineArrayMultiLine() throws IOException {
        System.out.println("Test: Inline Array (multi-line)");
        String input = "[3]:\n  a,b,c";
        ToonParser parser = new ToonParser(new StringReader(input));

        List<String> events = new ArrayList<>();
        ToonParser.Event event;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF) {
            events.add(event.toString());
        }

        System.out.println("  Input: " + input.replace("\n", "\\n"));
        System.out.println("  Events: " + events);
        assertEqual(5, events.size(), "Event count");
        assertEqual("START_ARRAY", events.get(0), "First event");
        assertEqual("END_ARRAY", events.get(4), "Last event");
        System.out.println("  ✓ Multi-line inline array works\n");
    }

    static void testTabularArray() throws IOException {
        System.out.println("Test: Tabular Array");
        String input = "[2]{id,name}:\n  1,Alice\n  2,Bob";
        ToonParser parser = new ToonParser(new StringReader(input));

        List<String> eventLog = new ArrayList<>();
        ToonParser.Event event;
        int count = 0;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count < 30) {
            String log = event.toString();
            if (event == ToonParser.Event.FIELD_NAME || event == ToonParser.Event.VALUE_STRING) {
                log += " (" + parser.getTextValue() + ")";
            } else if (event == ToonParser.Event.VALUE_NUMBER_INT) {
                log += " (" + parser.getNumberValue() + ")";
            }
            eventLog.add(log);
            System.out.println("    " + log);
            count++;
        }

        System.out.println("  Input: " + input.replace("\n", "\\n"));
        System.out.println("  Total events: " + eventLog.size());

        // Should have: START_ARRAY, START_OBJECT, FIELD(id), VALUE(1), FIELD(name), VALUE(Alice), END_OBJECT,
        //              START_OBJECT, FIELD(id), VALUE(2), FIELD(name), VALUE(Bob), END_OBJECT, END_ARRAY
        assertEqual("START_ARRAY", eventLog.get(0), "First event");
        System.out.println("  ✓ Tabular array works\n");
    }

    static void testListArray() throws IOException {
        System.out.println("Test: List Array");
        String input = "[2]:\n  - apple\n  - banana";
        ToonParser parser = new ToonParser(new StringReader(input));

        List<String> eventLog = new ArrayList<>();
        ToonParser.Event event;
        int count = 0;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count < 20) {
            String log = event.toString();
            if (event == ToonParser.Event.VALUE_STRING) {
                log += " (" + parser.getTextValue() + ")";
            }
            eventLog.add(log);
            System.out.println("    " + log);
            count++;
        }

        System.out.println("  Input: " + input.replace("\n", "\\n"));
        System.out.println("  Total events: " + eventLog.size());

        // Should have: START_ARRAY, VALUE(apple), VALUE(banana), END_ARRAY
        assertEqual("START_ARRAY", eventLog.get(0), "First event");
        assertEqual("END_ARRAY", eventLog.get(eventLog.size() - 1), "Last event");
        System.out.println("  ✓ List array works\n");
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
