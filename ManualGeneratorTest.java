import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Manual test for ToonGenerator - runs without JUnit
 */
public class ManualGeneratorTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== TOON Generator Manual Tests ===\n");

            testSimpleObject();
            testNestedObject();
            testInlineArray();
            testListArray();
            testRoundTrip();

            System.out.println("\n=== All generator tests passed! ===");
        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void testSimpleObject() throws IOException {
        System.out.println("Test: Simple Object Generation");
        StringWriter sw = new StringWriter();
        ToonGenerator gen = new ToonGenerator(sw);

        gen.writeStartObject();
        gen.writeFieldName("id");
        gen.writeNumber(123);
        gen.writeFieldName("name");
        gen.writeString("Alice");
        gen.writeEndObject();
        gen.flush();

        String output = sw.toString();
        System.out.println("Output:");
        System.out.println(output);

        // Verify output
        assertTrue(output.contains("id: 123"), "Should contain id field");
        assertTrue(output.contains("name: Alice"), "Should contain name field");
        System.out.println("? Simple object generation works\n");
    }

    static void testNestedObject() throws IOException {
        System.out.println("Test: Nested Object Generation");
        StringWriter sw = new StringWriter();
        ToonGenerator gen = new ToonGenerator(sw);

        gen.writeStartObject();
        gen.writeFieldName("user");
        gen.writeStartObject();
        gen.writeFieldName("id");
        gen.writeNumber(123);
        gen.writeFieldName("name");
        gen.writeString("Alice");
        gen.writeEndObject();
        gen.writeEndObject();
        gen.flush();

        String output = sw.toString();
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("user:"), "Should contain user field");
        assertTrue(output.contains("  id: 123"), "Should have indented id");
        System.out.println("? Nested object generation works\n");
    }

    static void testInlineArray() throws IOException {
        System.out.println("Test: Inline Array Generation");
        StringWriter sw = new StringWriter();
        ToonGenerator gen = new ToonGenerator(sw);

        gen.writeStartArray();
        gen.writeString("a");
        gen.writeString("b");
        gen.writeString("c");
        gen.writeEndArray();
        gen.flush();

        String output = sw.toString();
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("[3]:"), "Should have array header");
        assertTrue(output.contains("a,b,c") || output.contains("a, b, c"), "Should have inline values");
        System.out.println("? Inline array generation works\n");
    }

    static void testListArray() throws IOException {
        System.out.println("Test: List Array Generation");
        StringWriter sw = new StringWriter();
        ToonGenerator gen = new ToonGenerator(sw);

        gen.writeStartArray();
        for (int i = 0; i < 15; i++) {
            gen.writeString("item" + i);
        }
        gen.writeEndArray();
        gen.flush();

        String output = sw.toString();
        System.out.println("Output:");
        System.out.println(output);

        assertTrue(output.contains("[15]:"), "Should have array header");
        assertTrue(output.contains("- item"), "Should have list items");
        System.out.println("? List array generation works\n");
    }

    static void testRoundTrip() throws IOException {
        System.out.println("Test: Round Trip (Generate -> Parse)");

        // Generate TOON
        StringWriter sw = new StringWriter();
        ToonGenerator gen = new ToonGenerator(sw);

        gen.writeStartObject();
        gen.writeFieldName("id");
        gen.writeNumber(42);
        gen.writeFieldName("name");
        gen.writeString("Bob");
        gen.writeFieldName("active");
        gen.writeBoolean(true);
        gen.writeEndObject();
        gen.flush();

        String toon = sw.toString();
        System.out.println("Generated TOON:");
        System.out.println(toon);

        // Parse it back
        ToonParser parser = new ToonParser(new StringReader(toon));
        System.out.println("\nParsed events:");
        ToonParser.Event event;
        int count = 0;
        while ((event = parser.nextEvent()) != ToonParser.Event.EOF && count++ < 20) {
            System.out.print("  " + event);
            if (event == ToonParser.Event.FIELD_NAME || event == ToonParser.Event.VALUE_STRING) {
                System.out.print(" (" + parser.getTextValue() + ")");
            } else if (event == ToonParser.Event.VALUE_NUMBER_INT) {
                System.out.print(" (" + parser.getNumberValue() + ")");
            }
            System.out.println();
        }

        System.out.println("? Round trip works\n");
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }
}
