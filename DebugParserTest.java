import tools.jackson.dataformat.toon.*;
import java.io.*;

/**
 * Debug test for ToonParser
 */
public class DebugParserTest {
    public static void main(String[] args) {
        try {
            System.out.println("=== Debug Parser Test ===\n");

            String input = "name: Alice";
            ToonParser parser = new ToonParser(new StringReader(input));

            System.out.println("Input: " + input);
            System.out.println("\nEvents:");

            int count = 0;
            int maxEvents = 20; // Limit to prevent infinite loop
            ToonParser.Event event;
            while (count < maxEvents && (event = parser.nextEvent()) != ToonParser.Event.EOF) {
                System.out.printf("  %2d. %s", count + 1, event);
                if (event == ToonParser.Event.FIELD_NAME || event == ToonParser.Event.VALUE_STRING) {
                    System.out.print(" (" + parser.getTextValue() + ")");
                } else if (event == ToonParser.Event.VALUE_NUMBER_INT) {
                    System.out.print(" (" + parser.getNumberValue() + ")");
                }
                System.out.println();
                count++;
            }

            if (count >= maxEvents) {
                System.err.println("\n ERROR: Hit max event limit - possible infinite loop!");
                System.exit(1);
            }

            System.out.println("\nTotal events: " + count);
            System.out.println("✓ Test completed successfully");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
